package com.example.ledger;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final SettlementService settlementService;

    public PaymentController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    public ResponseEntity<?> createPayment(
            @Valid @RequestBody RecordPaymentRequest request
    ) {
       String paymentId = settlementService.recordPayment(
                request.merchantId(),
                request.amountMinor(),
                request.currency()
        );
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(paymentId)
                .toUri();
        return ResponseEntity.created(location).build();
    }



    @GetMapping("/settlement")
    public ResponseEntity<?> getSettlement(
            @RequestParam String merchantId
    ) {
        long settlementReturned = settlementService.settlementCalculator(merchantId);
        return ResponseEntity.ok(new SettlementResponse(settlementReturned));
    }



}
