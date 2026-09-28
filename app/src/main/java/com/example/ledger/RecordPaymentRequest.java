package com.example.ledger;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RecordPaymentRequest(
        @NotBlank String merchantId,
       @Positive long amountMinor,
      @NotBlank String currency) {

}
