package com.example.ledger;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class FeeTierService {

    private final MerchantFeeTierClient merchantFeeTierClient;
    private Map<String, BigDecimal> table;

    public FeeTierService(MerchantFeeTierClient merchantFeeTierClient) {
        this.merchantFeeTierClient = merchantFeeTierClient;
    }

    @PostConstruct
    public void load() {
        table = merchantFeeTierClient.fetchFeeTiers();
    }

    public BigDecimal rateFor(String tier) {
        return table.getOrDefault(tier, table.get("STANDARD"));
    }
}