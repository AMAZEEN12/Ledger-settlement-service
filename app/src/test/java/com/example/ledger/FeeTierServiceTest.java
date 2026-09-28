package com.example.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FeeTierServiceTest {

    @Test
    void returnsSameRateWhenUsedConcurrently() throws Exception {
        MerchantFeeTierClient client = new MerchantFeeTierClient();
        FeeTierService service = new FeeTierService(client);

        service.load();

        ExecutorService executor = Executors.newFixedThreadPool(20);

        try {
            List<Callable<BigDecimal>> tasks = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                tasks.add(() -> service.rateFor("STANDARD"));
            }

            List<Future<BigDecimal>> results = executor.invokeAll(tasks);

            for (Future<BigDecimal> result : results) {
                assertEquals(new BigDecimal("0.031"), result.get());
            }
        } finally {
            executor.shutdown();
        }
    }
}
