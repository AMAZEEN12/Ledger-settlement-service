package com.example.ledger.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(3)
public class DceBenchmark {

    long a;
    long b;
    long c;

    @Setup(Level.Trial)
    public void setUp() {
        a = 12450L;
        b = 31L;
        c = 7890L;
    }

    /**
     * DELIBERATELY BROKEN BENCHMARK:
     * Computes arithmetic work from state variables but discards the result.
     * The JIT compiler detects that 'result' is never read and has no observable side effects,
     * so Dead Code Elimination (DCE) removes the entire computation, leaving an empty method.
     */
    @Benchmark
    public void brokenDceDiscardResult() {
        long result = (a * b) + c;
        // Result is discarded; JIT eliminates this operation
    }

    /**
     * FIXED BENCHMARK (Variant 1 - Blackhole):
     * Consumes the computed result into JMH's Blackhole, forcing the JIT
     * to keep and execute the arithmetic calculation.
     */
    @Benchmark
    public void fixedDceBlackhole(Blackhole blackhole) {
        long result = (a * b) + c;
        blackhole.consume(result);
    }

    /**
     * FIXED BENCHMARK (Variant 2 - Return Value):
     * Returns the computed result directly from the benchmark method.
     * JMH automatically ensures the return value is observed by the harness.
     */
    @Benchmark
    public long fixedDceReturn() {
        return (a * b) + c;
    }

    /**
     * Legacy benchmark method name retained for backward compatibility with results.json.
     */
    @Benchmark
    public void calculateMethod(Blackhole blackhole) {
        long result = (a * b) + c;
        blackhole.consume(result);
    }
}
