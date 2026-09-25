package com.example.ledger;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SettlementService {
    private final PaymentRepository paymentRepository;

      public SettlementService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public String recordPayment(String merchantId, long amountMinor, String currency){
        PaymentEntity payment = new PaymentEntity();
        payment.merchantId = merchantId;
        payment.amountMinor = amountMinor;
        payment.currency = currency;
        payment.id = UUID.randomUUID().toString();
        payment.recordedAt = LocalDateTime.now();
        paymentRepository.save(payment);

        return payment.id;
    }

    @Value("${ledger.fee-rate}")
    private double feeRate;

    public long settlementCalculator(String merchantId){
        List<PaymentEntity> payments = paymentRepository.findByMerchantId(merchantId);
        if (payments.isEmpty()) {
            throw new UnknownMerchantException("Merchant " + merchantId + " does not exist");
        }
        long total = payments.stream().mapToLong(PaymentEntity::getAmountMinor).sum();
        long fee = (long) Math.floor(total * feeRate);
        return total - fee;

    }

}