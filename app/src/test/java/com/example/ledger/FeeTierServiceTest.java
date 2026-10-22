package com.example.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FeeTierServiceTest {

    static class FastCountingMerchantFeeTierClient extends MerchantFeeTierClient {
        final AtomicInteger fetchCount = new AtomicInteger(0);

        @Override
        public Map<String, BigDecimal> fetchFeeTiers() {
            fetchCount.incrementAndGet();
            return Map.of(
                    "STANDARD", new BigDecimal("0.031"),
                    "PREMIUM", new BigDecimal("0.021")
            );
        }
    }

    @Test
    void returnsSameRateWhenUsedConcurrently() throws Exception {
        FastCountingMerchantFeeTierClient client = new FastCountingMerchantFeeTierClient();
        FeeTierService service = new FeeTierService(client);
        service.load();

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Future<BigDecimal>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    return service.rateFor("STANDARD");
                }));
            }

            // Release all threads simultaneously for true concurrency
            startLatch.countDown();

            for (Future<BigDecimal> future : futures) {
                assertEquals(new BigDecimal("0.031"), future.get(5, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void loadsTableExactlyOnce() throws Exception {
        FastCountingMerchantFeeTierClient client = new FastCountingMerchantFeeTierClient();
        FeeTierService service = new FeeTierService(client);

        // Load at startup
        service.load();

        int concurrentReaders = 25;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentReaders);
        try {
            List<Callable<BigDecimal>> tasks = new ArrayList<>();
            for (int i = 0; i < concurrentReaders; i++) {
                tasks.add(() -> service.rateFor("STANDARD"));
            }
            List<Future<BigDecimal>> results = executor.invokeAll(tasks);
            for (Future<BigDecimal> res : results) {
                assertEquals(new BigDecimal("0.031"), res.get());
            }

            // Verify the downstream fetch was called exactly once during initialization
            assertEquals(1, client.fetchCount.get(), "Fee table must be loaded exactly once");
        } finally {
            executor.shutdown();
        }
    }
}
