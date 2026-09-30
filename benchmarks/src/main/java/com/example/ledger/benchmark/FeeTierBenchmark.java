package com.example.ledger.benchmark;

import com.example.ledger.FeeTierService;
import com.example.ledger.MerchantFeeTierClient;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(3)
public class FeeTierBenchmark {

    MerchantFeeTierClient merchantFeeTierClient;
    FeeTierService feeTierService;
    String tier;

    @Setup
    public void setUp() {
        tier = "STANDARD";

        merchantFeeTierClient = new MerchantFeeTierClient();
        feeTierService = new FeeTierService(merchantFeeTierClient);
        feeTierService.load();
    }

    @Benchmark
    public void benchMarkMeasurement(Blackhole blackhole) {
        blackhole.consume(feeTierService.rateFor(tier));
    }
}