package com.ebookstore.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Order id is required")
    private Long orderId;

    @NotBlank(message = "Payment method is required")
    private String method; // CREDIT_CARD or DEBIT_CARD

    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
}

