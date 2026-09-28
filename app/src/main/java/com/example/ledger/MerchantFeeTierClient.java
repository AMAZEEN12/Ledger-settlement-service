package com.example.ledger;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MerchantFeeTierClient {

    public Map<String, Double> fetchFeeTiers() {
        try {
            Thread.sleep(3000); // simulates a slow call to a downstream fee-schedule service
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return Map.of(
                "STANDARD", 0.031,
                "PREMIUM", 0.021
        );
    }
}