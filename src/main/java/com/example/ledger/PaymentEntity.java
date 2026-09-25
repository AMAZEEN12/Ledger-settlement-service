package com.example.ledger;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class PaymentEntity {
    @Id
    String id;
    String merchantId;
    long amountMinor;
    String currency;
    LocalDateTime recordedAt;

    // JPA entities are mutable and require a no-argument constructor, so this is a class rather than a record.
    public PaymentEntity(){

    }
    public long getAmountMinor(){
        return amountMinor;
    }
}
