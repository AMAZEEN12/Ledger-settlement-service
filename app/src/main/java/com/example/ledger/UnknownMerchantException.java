package com.example.ledger;

public class UnknownMerchantException extends RuntimeException {
    public UnknownMerchantException(String message) {
        super(message);
    }
}
