package com.example.ledger.benchmark;

import com.example.ledger.SettlementResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(3)
public class SettlementResponseBenchmark {

    SettlementResponse settlementResponse;
    ObjectMapper objectMapper;

    @Setup
    public void setUp() {
        settlementResponse = new SettlementResponse(124469L);
        objectMapper = new ObjectMapper();
    }

    @Benchmark
    public void benchmarkMeasurement(Blackhole blackhole) throws Exception {
        blackhole.consume(objectMapper.writeValueAsString(settlementResponse));
    }
}