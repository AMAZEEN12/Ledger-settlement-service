package com.example.ledger;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FeeTierService {

    private final MerchantFeeTierClient merchantFeeTierClient;
    private Map<String, Double> table;

    public FeeTierService(MerchantFeeTierClient merchantFeeTierClient) {
        this.merchantFeeTierClient = merchantFeeTierClient;
    }

    @PostConstruct
    void load() {
        table = merchantFeeTierClient.fetchFeeTiers();
    }

    double rateFor(String tier) {
        return table.getOrDefault(tier, table.get("STANDARD"));
    }
}