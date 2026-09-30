package com.example.ledger.benchmark;

import com.example.ledger.FeeTierService;
import com.example.ledger.MerchantFeeTierClient;
import com.example.ledger.PaymentEntity;
import com.example.ledger.PaymentRepository;
import com.example.ledger.SettlementService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(3)
public class SettlementBenchMark {

    SettlementService settlementService;
    String merchantId;

    @Setup
    public void setUp() {
        merchantId = "MR-4471";

        PaymentEntity payment1 = new BenchmarkPayment(50000L);
        PaymentEntity payment2 = new BenchmarkPayment(75000L);
        PaymentEntity payment3 = new BenchmarkPayment(24950L);

        List<PaymentEntity> payments =
                List.of(payment1, payment2, payment3);

        PaymentRepository paymentRepository =
                (PaymentRepository) Proxy.newProxyInstance(
                        PaymentRepository.class.getClassLoader(),
                        new Class<?>[]{PaymentRepository.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("findByMerchantId")) {
                                return payments;
                            }

                            throw new UnsupportedOperationException(
                                    "Benchmark repository method not supported: "
                                            + method.getName()
                            );
                        }
                );

        MerchantFeeTierClient merchantFeeTierClient =
                new MerchantFeeTierClient();

        FeeTierService feeTierService =
                new FeeTierService(merchantFeeTierClient);

        feeTierService.load();

        settlementService =
                new SettlementService(paymentRepository, feeTierService);
    }

    @Benchmark
    public void benchmarkMeasurement(Blackhole blackhole) {
        blackhole.consume(
                settlementService.settlementCalculator(merchantId)
        );
    }

    private static class BenchmarkPayment extends PaymentEntity {

        private final long amountMinor;

        BenchmarkPayment(long amountMinor) {
            this.amountMinor = amountMinor;
        }

        @Override
        public long getAmountMinor() {
            return amountMinor;
        }
    }
}