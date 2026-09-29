# Ledger Settlement Service

A Spring Boot service providing payment recording (`POST /payments`) and merchant settlement calculations (`GET /payments/settlement`).

## Prerequisites
- Java 25 (or Java 26)
- Maven 3.9+

---

## 🚀 Building and Running Benchmarks (Single Command)

To build the `benchmarks` Maven module and run the JMH benchmark suite in a single command, execute:

```powershell
mvn clean package -pl benchmarks -am -DskipTests; java -jar benchmarks/target/benchmarks.jar -rf json -rff results.json
```

Or on Linux / macOS / Bash:

```bash
mvn clean package -pl benchmarks -am -DskipTests && java -jar benchmarks/target/benchmarks.jar -rf json -rff results.json
```

To include the GC allocation profiler:

```powershell
mvn clean package -pl benchmarks -am -DskipTests; java -jar benchmarks/target/benchmarks.jar -prof gc -rf json -rff results.json
```

---

## 📚 Lab Deliverables & Documentation

### 1. Virtual Threads Migration & Pinning Hunt
- **Documentation:** [`docs/threads.md`](docs/threads.md)
- **Branch:** `feature/virtual-threads`
- **JFR Recordings:** `pinned.jfr` (pinned event trace) and `fixed.jfr` (0 pinning events)
- **PR Scope:** Under 400 changed lines (280 lines total) with rollback instructions.

### 2. Garbage Collection Under Load
- **Documentation:** [`docs/gc.md`](docs/gc.md)
- **Branch:** `feature/gc-tuning`
- **Load Test Script:** [`perf/steady-load.js`](perf/steady-load.js)
- **JFR Recordings:** `gc-baseline.jfr` and `gc-tuned.jfr`
- **Tuning Flag:** `-Xmx3g`

### 3. JMH Benchmark Suite
- **Documentation:** [`docs/benchmarks.md`](docs/benchmarks.md)
- **Branch:** `feature/jmh-benchmarks`
- **Module:** [`benchmarks`](benchmarks/)
- **Results Evidence:** `results.json`
