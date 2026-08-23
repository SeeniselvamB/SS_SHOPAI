package com.ss.shopai.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockPaymentRequest {

    @NotNull(message = "Order id is required")
    private Long orderId;

    /**
     * Simulates payment gateway outcome for testing/demo purposes.
     * If true (or omitted), payment succeeds; if false, it fails.
     */
    @Builder.Default
    private Boolean simulateSuccess = true;
}