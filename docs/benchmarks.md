# JMH Benchmark Suite

## 1. Purpose

This document records the JMH benchmark work performed on the Ledger settlement service.

The benchmarks were designed to measure three representative areas of the application:

1. **Serialization** — converting a `SettlementResponse` object to JSON using Jackson.
2. **String/collection-heavy work** — looking up a fee tier from the `FeeTierService` using a `String` key in a `Map`.
3. **Settlement calculation** — calculating the merchant settlement amount using payment amounts stored as `long` minor units and fees calculated with `BigDecimal`.

The benchmarks use JMH so that JVM warm-up, repeated measurements, and multiple JVM forks are handled consistently.

---

## 2. Benchmark Selection

### 2.1 SettlementResponse serialization

The `SettlementResponse` record is serialized to JSON using Jackson:

```text
SettlementResponse object
        ↓
     Jackson
        ↓
     JSON String
```

This was selected because serialization is explicitly one of the required benchmark categories and represents work performed when returning API responses.

The benchmark uses the real `SettlementResponse` class from the Ledger application.

### 2.2 Fee tier lookup

The `FeeTierService.rateFor()` method was selected because it performs a `String`-keyed lookup in the fee-tier map.

The benchmark uses the real `FeeTierService` and `MerchantFeeTierClient` classes. The fee-tier table is populated during `@Setup` so that the 3-second simulated external call is not included in the measured operation.

The measured operation is the lookup itself:

```text
rateFor("STANDARD")
        ↓
Map lookup
        ↓
BigDecimal fee rate
```

### 2.3 Settlement calculation

`SettlementService.settlementCalculator()` was selected because it is a central business calculation in the application.

The calculation:

* obtains the merchant's payments,
* sums payment amounts using `long` minor units,
* converts the total to `BigDecimal`,
* calculates the fee using the configured `BigDecimal` rate,
* applies `RoundingMode.FLOOR`,
* returns the final settlement amount as a `long`.

The benchmark uses an in-memory repository proxy rather than a live database. This keeps database I/O out of the measurement and allows the benchmark to focus on the calculation itself.

---

## 3. Benchmark Configuration

The final benchmarks use the following JMH configuration:

```java
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(3)
```

### Configuration rationale

* `Scope.Benchmark` gives each benchmark its own benchmark state.
* `AverageTime` measures the average time taken per operation.
* Results are reported in microseconds per operation.
* Five warm-up iterations allow the JVM to reach a more stable optimized state before measurement.
* Ten measurement iterations provide repeated measurements for each fork.
* Three forks run the benchmark in separate JVM processes.

**Why forks matter:** separate JVM forks reduce the chance that previous benchmark runs, JVM state, JIT compilation history, or other benchmark state affects the results.

Inputs used by the benchmarks are placed in benchmark state and initialized in `@Setup` rather than being created as constants inside the measured methods. This reduces the risk of constant folding and other JVM optimizations changing what is actually measured.

---

## 4. Dead-Code Elimination Experiment

A deliberately incorrect benchmark was first created to demonstrate the danger of benchmarking work whose result is discarded.

The broken benchmark calculated a value but did not use the result:

```java
@Benchmark
public void calculateMethod() {
    long calculateSomething = 12 * 14;
}
```

The benchmark produced:

```text
2289261640.643 ± 784225634.451 ops/s
```

This result was suspiciously fast because the calculated value was not observable outside the method.

### Fixed benchmark

The result was then passed to JMH's `Blackhole`:

```java
@Benchmark
public void calculateMethod(Blackhole blackhole) {
    long calculateSomething = 12 * 14;
    blackhole.consume(calculateSomething);
}
```

The quick comparison produced:

```text
2459110403.520 ± 152759600.617 ops/s
```

The fixed run did **not** become slower in this particular experiment. Therefore, the results do not provide a clean numerical measurement of a DCE speed-up or slow-down.

The experiment nevertheless demonstrates why benchmark results should not be trusted when the computed result is simply discarded. The final benchmark consumes its result using `Blackhole`.

The final state-based DCE benchmark produced:

```text
0.001 ± 0.001 us/op
```

This benchmark is intentionally simple and is included primarily as the negative/control example for the DCE experiment.

---

## 5. Final Benchmark Results

The following results are from the final normal JMH suite:

```text
DceBenchmark.calculateMethod
0.001 ± 0.001 us/op

FeeTierBenchmark.benchMarkMeasurement
0.013 ± 0.001 us/op

SettlementBenchMark.benchmarkMeasurement
0.068 ± 0.001 us/op

SettlementResponseBenchmark.benchmarkMeasurement
0.177 ± 0.005 us/op
```

### Error-bar table

| Benchmark                        | Score ± Error | Unit  |
| -------------------------------- | ------------: | ----- |
| DCE benchmark                    | 0.001 ± 0.001 | µs/op |
| Fee tier lookup                  | 0.013 ± 0.001 | µs/op |
| Settlement calculation           | 0.068 ± 0.001 | µs/op |
| SettlementResponse serialization | 0.177 ± 0.005 | µs/op |

The error values are the JMH reported measurement errors.

The benchmark results should not be interpreted as an overall ranking because the benchmarks represent different workloads and perform different amounts and types of work.

---

## 6. Allocation Results

The allocation measurements were collected using the JMH GC profiler:

```text
java -jar benchmarks\target\benchmarks.jar -prof gc -rf json -rff results.json
```

The exact allocation results extracted from `results.json` were:

| Benchmark                        |        Allocation ± Error | Unit |
| -------------------------------- | ------------------------: | ---- |
| DCE benchmark                    | 0.000000411 ± 0.000000007 | B/op |
| Fee tier lookup                  | 0.000008342 ± 0.000000549 | B/op |
| Settlement calculation           |           200.000 ± 0.001 | B/op |
| SettlementResponse serialization |           536.000 ± 0.001 | B/op |

The first two allocation values are extremely small. JMH displayed them in approximate scientific notation in the Windows console, which caused the exponent characters to appear as `??`. The exact numeric values were therefore extracted directly from `results.json`.

The settlement calculation allocated approximately 200 bytes per operation in the profiler run, while the Jackson serialization benchmark allocated approximately 536 bytes per operation.

The allocation measurements are characteristics of the individual workloads and should not be treated as a ranking between unrelated operations.

---

## 7. Profiler Results

The GC profiler also reported the following measured times:

| Benchmark                        |    GC-profiler time |                     Allocation |
| -------------------------------- | ------------------: | -----------------------------: |
| DCE benchmark                    | 0.001 ± 0.001 µs/op | 0.000000411 ± 0.000000007 B/op |
| Fee tier lookup                  | 0.012 ± 0.001 µs/op | 0.000008342 ± 0.000000549 B/op |
| Settlement calculation           | 0.090 ± 0.016 µs/op |           200.000 ± 0.001 B/op |
| SettlementResponse serialization | 0.229 ± 0.033 µs/op |           536.000 ± 0.001 B/op |

The GC-profiler timings are reported separately from the normal benchmark results because enabling a profiler can introduce measurement overhead. Therefore, the normal suite is used as the primary timing result, while the profiler run is used to examine allocation behaviour.

---

## 8. Comparison and Uncertainty

The benchmark results show measurable differences between the workloads, but the measurements should be interpreted within the context of what each benchmark actually does.

For example, the settlement calculation and serialization benchmarks perform substantially different operations. The settlement benchmark processes a collection of payments and performs arithmetic, while the serialization benchmark converts an object into JSON.

A comparison between two measurements whose error intervals overlap should be treated as **inconclusive** rather than declaring one operation definitively faster.

In particular, the DCE benchmark's result is extremely close to the resolution of the measurement and has an error equal to its reported score. Therefore, its result should not be used for a meaningful performance comparison with the application benchmarks.

The DCE experiment is instead useful as a control demonstrating the importance of making benchmark results observable.

---

## 9. Limitations

### 9.1 Concurrency

These benchmarks measure individual operations in a controlled JMH environment. They do not reproduce the concurrency characteristics of the production service under many simultaneous HTTP requests.

Thread contention, request queues, connection-pool contention, and scheduling effects may therefore produce different results in production.

### 9.2 Production-sized data

The settlement benchmark uses a small, fixed in-memory set of three payments:

```text
50000
75000
24950
```

Real merchants may have much larger payment histories. Cache behaviour, collection traversal, memory usage, and garbage collection could change as the number and size of payments increase.

### 9.3 Benchmark harness versus live request path

The settlement benchmark does not exercise the complete production request path.

It does not include:

* HTTP request handling,
* Spring MVC processing,
* request validation,
* database network I/O,
* database query execution,
* connection-pool behaviour,
* JSON response handling.

Instead, the repository is represented by an in-memory proxy so that the settlement calculation itself can be measured without database latency.

### 9.4 Profiler overhead

The GC profiler adds instrumentation and therefore may affect measured timings. For this reason, the normal JMH run is treated as the primary timing measurement and the profiler run is used mainly for allocation analysis.

### 9.5 JVM and machine conditions

JMH results depend on the JVM, operating system, hardware, background processes, CPU power-management behaviour, and other environmental factors. Results should therefore be treated as measurements of this benchmark environment rather than universal performance guarantees.

---

## 10. Commands Used

### Build the benchmark module

```powershell
mvn -pl benchmarks -am clean package -DskipTests
```

### Run the final benchmark suite

```powershell
java -jar benchmarks\target\benchmarks.jar -rf json -rff results.json
```

### Run with the GC allocation profiler

```powershell
java -jar benchmarks\target\benchmarks.jar -prof gc -rf json -rff results.json
```

The final `results.json` contains the JMH benchmark results and profiler measurements used in this document.

---

## 11. Summary

The benchmark suite demonstrates how JMH can be used to measure representative operations in the Ledger settlement service while controlling JVM warm-up, measurement iterations, and JVM forks.

The final suite contains:

* a deliberate DCE control benchmark,
* a fee-tier map lookup,
* the settlement calculation,
* Jackson serialization of the settlement response.

The results show that the benchmarks have different execution and allocation characteristics. The measurements should be interpreted according to the specific workload being tested rather than treated as a universal ranking of application performance.

The settlement benchmark in particular should be considered a measurement of the settlement calculation under an in-memory repository setup, not a measurement of the complete production settlement request.
