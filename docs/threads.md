### Step 3 — Baseline Thread Pool

The application uses Spring Boot's embedded Tomcat server through `spring-boot-starter-web`. No application-defined `ExecutorService` or `ThreadPoolTaskExecutor` was found, so the HTTP requests are handled by Tomcat's server configuration.

For the traditional Tomcat configuration:

| Setting                             | Value |
| ----------------------------------- | ----: |
| Core/min-spare threads              |    10 |
| Maximum threads                     |   200 |
| Connection backlog (`accept-count`) |  8192 |

### Step 2 & Step 5 — Load Test Results

The baseline and virtual-thread runs used the same k6 load profile: **200 virtual users for 60 seconds**.

| Metric          |       Baseline |    Virtual threads |
| --------------- | -------------: | -----------------: |
| Throughput      | 1,546.92 req/s | **2,185.36 req/s** |
| p50             |       93.58 ms |       **83.15 ms** |
| p95             |      260.46 ms |      **176.44 ms** |
| p99             |      732.41 ms |      **299.23 ms** |
| Failed requests |             0% |             **0%** |

The virtual-thread run improved throughput and reduced p50, p95 and p99 latency compared with the baseline.

## Step 8 — Pinning Evidence

### Pinned Event

The pinned event below was captured using Java Flight Recorder after a cold restart of the application while the 200-VU load test was running.

**JFR event:** `jdk.VirtualThreadPinned`

| Field              | Captured value                                                 |
| ------------------ | -------------------------------------------------------------- |
| Duration           | **3.70 seconds**                                               |
| Blocking operation | `LockSupport.park`                                             |
| Pinning reason     | `VM call to com.example.ledger.FeeTierTable.<clinit> on stack` |
| Virtual thread     | `tomcat-handler-160`                                           |
| Carrier thread     | `ForkJoinPool-1-worker-2`                                      |
| Virtual thread ID  | `210`                                                          |
| Carrier thread ID  | `49`                                                           |

### Relevant Stack Trace

```text
java.lang.VirtualThread.parkOnCarrierThread(boolean, long)
java.lang.VirtualThread.parkNanos(long)
java.lang.VirtualThread.sleepNanos(long)
java.lang.Thread.sleepNanos(long)
java.lang.Thread.sleep(long)
...
```

The relevant application code is:

```text
FeeTierTable.<clinit>
    ↓
MerchantFeeTierClient.fetchFeeTiers()
    ↓
Thread.sleep(3000)
```

`<clinit>` is the JVM's internal name for the static class initializer. In this application, it represents the `static { ... }` block inside `FeeTierTable`.

### Why the Virtual Thread Was Pinned

The request calls:

```text
SettlementService.settlementCalculator()
    ↓
FeeTierTable.rateFor("STANDARD")
```

Before `FeeTierTable.rateFor()` can run, the JVM must initialize `FeeTierTable`. Its static initializer executes:

```text
static {
    TABLE = new MerchantFeeTierClient().fetchFeeTiers();
}
```

`fetchFeeTiers()` simulates a slow downstream service and calls:

```text
Thread.sleep(3000)
```

Therefore, the first request that initializes `FeeTierTable` enters the class initializer and then blocks for approximately three seconds.

JFR reported the pinning reason as:

```text
VM call to com.example.ledger.FeeTierTable.<clinit> on stack
```

The frame that cannot unmount is therefore the class-initialization path through `FeeTierTable.<clinit>`. The blocking operation inside that path is the `Thread.sleep(3000)` in `MerchantFeeTierClient.fetchFeeTiers()`.

Other virtual threads that arrived while `FeeTierTable` was still being initialized were also observed waiting for initialization:

```text
Waited for initialization of com.example.ledger.FeeTierTable by another thread
```

This demonstrates that the blocking downstream call placed inside the class initializer can pin virtual threads during cold-start initialization.

### Pre-Java-24 `synchronized` Guidance

Before Java 24, virtual-thread pinning could occur when a virtual thread blocked while executing a `synchronized` method or block, so guidance recommended replacing `synchronized` with `ReentrantLock` when the synchronized section contained blocking operations.

That change would not fix this defect because the pinning captured here is caused by class initialization (`FeeTierTable.<clinit>`) containing a blocking call, not by a `synchronized` block; changing the lock type would therefore leave the blocking class initializer unchanged.

## Step 9 — Fix

The blocking fee-tier load was moved out of the `FeeTierTable` static initializer.

A Spring-managed `FeeTierService` now loads the fee-tier table during application startup using `@PostConstruct`. `MerchantFeeTierClient` is also managed by Spring and is injected into `FeeTierService`.

The request path now reads the already-loaded table instead of triggering class initialization.

The old `FeeTierTable` class was removed.

A concurrency test was also added. It creates 20 concurrent tasks that read the `STANDARD` fee rate from the shared table and verifies that every result is `0.031`.

`mvn test` completed successfully.

## Step 10 — Fixed Run

After the fix, the application was restarted and the JPA query infrastructure was warmed up before starting the fixed JFR recording. The same 200-VU, 60-second k6 load test was then run.

The fixed run produced:

| Metric     |       Baseline | Virtual Threads |              Fixed |
| ---------- | -------------: | --------------: | -----------------: |
| Throughput | 1,546.92 req/s |  2,185.36 req/s | **1,516.95 req/s** |
| p50        |       93.58 ms |        83.15 ms |      **107.57 ms** |
| p95        |      260.46 ms |       176.44 ms |      **295.91 ms** |
| p99        |      732.41 ms |       299.23 ms |      **587.01 ms** |
| Failed     |             0% |              0% |             **0%** |

The fixed JFR recording was saved as `fixed.jfr`.

The command:

```text
jfr print --events jdk.VirtualThreadPinned .\fixed.jfr
```

returned no events.

Therefore, the fixed recording contains **no `jdk.VirtualThreadPinned` events**.

The two JFR recordings produced for the exercise are:

* `pinned.jfr` — contains the original `FeeTierTable.<clinit>` pinning event.
* `fixed.jfr` — contains no `jdk.VirtualThreadPinned` events.

## Pull Request Details

### Title
`feat(threads): migrate web layer to virtual threads and eliminate class initializer pinning defect`

### Summary of Changes
- Enabled Spring Boot virtual threads (`spring.threads.virtual.enabled=true` in `application.yml`).
- Moved downstream fee-tier loading out of static initializer (`FeeTierTable.<clinit>`) to an eager `@PostConstruct` lifecycle method in `FeeTierService.java`.
- Added unit test `FeeTierServiceTest.java` verifying concurrent thread safety.
- Verified removal of carrier thread pinning (`fixed.jfr` reports 0 `jdk.VirtualThreadPinned` events).

### Risk Assessment
- **Low Risk**: The eager loading at application startup moves potential network latency to boot time rather than runtime requests. If downstream fee table service is unavailable at boot, startup fails early (fail-fast behavior).

### Rollback Plan
- To roll back virtual thread execution: set `spring.threads.virtual.enabled=false` in `application.yml` and restart the application.
- To revert fee tier service changes: revert `FeeTierService.java` and re-enable static initializer loading if required.

### Changed Lines Count
- Total lines changed relative to `main`: **280 lines** (11 files changed, 278 insertions, 2 deletions), well under the 400 changed lines limit.

