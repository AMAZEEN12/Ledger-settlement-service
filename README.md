# Ledger Settlement Service

A Spring Boot financial service providing ledger payment recording (`POST /payments`) and merchant settlement calculations (`GET /payments/settlement`).

## Prerequisites & Environment
- **Java:** JDK 25 or approved JDK 26 LTS (`java -version`: Java SE 26.0.2.1+1-7, 64-Bit Server VM)
- **Build Tool:** Apache Maven 3.9+ (`mvn -v`: Maven 3.9.16)
- **OS:** Windows / Linux / macOS

---

## 🚀 Building and Running Benchmarks (Single Command)

Per criterion **B1**, the entire JMH benchmark module can be built from a clean clone and executed from repository root in a single command without manual steps:

### Windows (PowerShell):
```powershell
mvn clean package -pl benchmarks -am -DskipTests; java -jar benchmarks/target/benchmarks.jar -rf json -rff results.json
```

### Windows (cmd.exe):
```cmd
mvn clean package -pl benchmarks -am -DskipTests && java -jar benchmarks\target\benchmarks.jar -rf json -rff results.json
```

### Linux / macOS (Bash):
```bash
mvn clean package -pl benchmarks -am -DskipTests && java -jar benchmarks/target/benchmarks.jar -rf json -rff results.json
```

### Single Command with GC Allocation Profiler:
```powershell
mvn clean package -pl benchmarks -am -DskipTests; java -jar benchmarks/target/benchmarks.jar -prof gc -rf json -rff results.json
```

### Benchmark Run Durations
- **Full Measurement Suite (3 Forks, 5 Warmups, 10 Iterations):** Approximately **8 to 10 minutes** on standard hardware (Intel Core i3, 4 logical processors).
- **Quick Verification / Test Mode (1 Fork, 2 Warmups, 3 Iterations):** Approximately **1.5 to 2 minutes**:
  ```powershell
  java -jar benchmarks/target/benchmarks.jar -f 1 -wi 2 -i 3 -rf json -rff results.json
  ```

---

## 📚 Lab Deliverables & Documentation Index

### 1. SJV-L1 — Virtual Threads Migration & Pinning Hunt
- **Documentation:** [`docs/threads.md`](docs/threads.md)
- **Branch:** `feature/virtual-threads`
- **JFR Recordings:**
  - `pinned.jfr` — contains captured `jdk.VirtualThreadPinned` events (26 events, `<clinit>` defect).
  - `fixed.jfr` — verified 0 `jdk.VirtualThreadPinned` events following eager startup initialization.
- **Pull Request Scope:** Under 400 changed lines with production risk assessment and rollback plan.
- **Load Test Script:** [`loadtest/baseline.js`](loadtest/baseline.js) (200 VUs, 60s).

### 2. SJV-L2 — JVM Performance & Garbage Collection Tuning
- **Documentation:** [`docs/gc.md`](docs/gc.md)
- **Branch:** `feature/gc-tuning`
- **Starting Settings:** Ergonomic `MaxHeapSize = 2107637760` (~2 GiB), `UseG1GC = true`.
- **Tuning Lever:** Exactly one variable changed: `-Xmx3g` (heap expanded to 3 GiB).
- **Load Test Script:** [`perf/steady-load.js`](perf/steady-load.js) (10-minute constant-arrival-rate test).
- **JFR Recordings:**
  - `gc-baseline.jfr` (10-minute recording at 2 GiB default).
  - `gc-tuned.jfr` (10-minute recording at 3 GiB tuned).

### 3. SJV-L3 — JMH Microbenchmarks
- **Documentation:** [`docs/benchmarks.md`](docs/benchmarks.md)
- **Branch:** `feature/jmh-benchmarks`
- **Module:** [`benchmarks/`](benchmarks/) (isolated Maven module with `jmh-core` and maven-shade-plugin).
- **Raw Evidence:** [`results.json`](results.json)
- **Deliberately Broken vs. Fixed Benchmark:** [`DceBenchmark.java`](benchmarks/src/main/java/com/example/ledger/benchmark/DceBenchmark.java) demonstrating Dead Code Elimination (DCE) prevention.
- **Real Application Workloads:**
  - `SettlementResponseBenchmark.java` (Jackson JSON serialization)
  - `FeeTierBenchmark.java` (String-keyed Map lookup)
  - `SettlementBenchMark.java` (Core financial calculation on `long` minor units and `BigDecimal`)
