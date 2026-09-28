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
    long calculateSomething;
    @Setup
    public void setUp(){
    calculateSomething = 12 * 14;
    }

    @Benchmark

    public void calculateMethod(Blackhole blackhole){

        blackhole.consume(calculateSomething);

    }
}
