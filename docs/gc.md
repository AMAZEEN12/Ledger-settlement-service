# Garbage Collection Under Load

## Baseline Configuration

- Java: 25.0.4.1 LTS
- Garbage collector: G1GC
- Baseline MaxHeapSize: approximately 2 GiB
- Load target: 1200 requests/second
- Test duration: 10 minutes
- k6 maximum VUs: 400
- Endpoint: `/payments/settlement?merchantId=MR-4471`

The JVM selected a maximum heap of 2,107,637,760 bytes (~2 GiB)
ergonomically, and G1GC was enabled.

## Baseline JFR Findings

The baseline JFR recording showed the following:

| Metric | Baseline |
|---|---:|
| Allocation rate* | ~28.5 MB/s |
| GC collections | 194 |
| Longest GC pause | 27.550 ms |

### Top Allocating Classes

| Class | Alloc Total | Total Allocation |
|---|---:|---:|
| `jdk.internal.vm.StackChunk` | 3.44 GiB | 18.4% |
| `byte[]` | 2.08 GiB | 11.2% |
| `java.lang.Object[]` | 1.21 GiB | 6.49% |

The top three allocating classes accounted for approximately 36.1% of
the recorded allocation.

\*The allocation rate is based on a representative 30-second JMC
interval rather than the entire recording.

## Problem Classification

The baseline shows a modest allocation rate of approximately 28.5 MB/s,
while the allocation profile is led by JVM-internal classes rather than
application-domain objects. The longest observed GC pause was 27.550 ms.
Based on these observations, the experiment was treated as a pause-oriented
GC problem rather than an application-allocation problem.

## Tuning Change

The heap size was increased from approximately 2 GiB to 3 GiB.
No other JVM tuning variable was changed. G1GC remained enabled.

The tuned JVM was started with:

`-Xmx3g`

## Tuned JFR Findings

| Metric | Tuned |
|---|---:|
| Allocation rate* | ~59.4 MB/s |
| GC collections | 163 |
| Longest GC pause | 22.090 ms |

### Top Allocating Classes

| Class | Alloc Total | Total Allocation |
|---|---:|---:|
| `jdk.internal.vm.StackChunk` | 4.485 GB | 20.98% |
| `byte[]` | 2.360 GB | 11.04% |
| `java.lang.Object[]` | 1.304 GB | 6.10% |

The top three allocating classes accounted for approximately 38.13% of
the recorded allocation.

\*The allocation rate is based on a representative 30-second JMC
interval rather than the entire recording.

## Baseline vs Tuned Results

| Metric | Baseline | Tuned |
|---|---:|---:|
| Allocation rate* | ~28.5 MB/s | ~59.4 MB/s |
| GC collections | 194 | 163 |
| Longest GC pause | 27.550 ms | 22.090 ms |
| p99 latency | 410.96 ms | 425.91 ms |
| Throughput | 1176.14 req/s | 1182.40 req/s |
| HTTP request failures | 0.02% | 0.00% |
| Dropped iterations | 14,311 | 10,546 |

\*Allocation rates are based on representative 30-second JMC intervals,
not whole-recording averages.

### Observations

Increasing the maximum heap from approximately 2 GiB to 3 GiB reduced
the number of GC collections from 194 to 163 and reduced the longest
observed pause from 27.550 ms to 22.090 ms.

The p99 latency increased from 410.96 ms to 425.91 ms, so the experiment
did not demonstrate an improvement in p99 latency. Throughput increased
slightly from 1176.14 req/s to 1182.40 req/s, while the tuned run had no
HTTP request failures.

Both runs reached the k6 limit of 400 active VUs and reported dropped
iterations. Therefore, the measured throughput was below the requested
1200 iterations/second target, and the results should be interpreted
with that load-generation limitation in mind.

## Trade-off

The 3 GiB heap reduced GC collection count from 194 to 163 and reduced
the longest observed pause from 27.550 ms to 22.090 ms, but p99 latency
increased from 410.96 ms to 425.91 ms and the sampled allocation rate
was higher.

The trade-off is acceptable for this experiment because the tuning reduced
GC frequency and the longest observed pause without changing the collector.
The additional memory cost is acceptable when the deployment environment
has sufficient memory and the priority is reducing GC pauses rather than
improving p99 latency.

## Rollback

The tuning change can be rolled back by removing:

`-Xmx3g`

This returns the JVM to its previous ergonomically selected heap size
of approximately 2 GiB while retaining G1GC.

## Artifacts

- `gc-baseline.jfr`
- `gc-tuned.jfr`
- `perf/steady-load.js`

Additional p99 validation recordings were generated during the experiment:

- `gc-baseline-p99.jfr`
- `gc-tuned-p99.jfr`