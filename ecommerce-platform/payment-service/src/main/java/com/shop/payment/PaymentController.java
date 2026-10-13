package com.shop.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** Internal only: called by order-service, never exposed through the gateway. */
@RestController
@RequestMapping("/internal/payments")
public class PaymentController {

    public record PaymentRequest(@NotNull Long orderId, @NotNull @Positive BigDecimal amount) {}
    public record PaymentResult(String status, Long paymentId) {}

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public PaymentResult pay(@Valid @RequestBody PaymentRequest r) {
        Payment p = service.charge(r.orderId(), r.amount());
        return new PaymentResult(p.getStatus(), p.getId());
    }
}
