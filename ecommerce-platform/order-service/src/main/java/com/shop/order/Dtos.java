package com.shop.order;

import java.math.BigDecimal;

/** Shapes of the other services' responses/requests, as seen by order-service. */
public final class Dtos {
    private Dtos() {}

    public record CartItemDto(Long productId, int quantity) {}
    public record ProductDto(Long id, String name, BigDecimal price, int stock) {}
    public record PaymentRequest(Long orderId, BigDecimal amount) {}
    public record PaymentResult(String status, Long paymentId) {}
    public record NotificationRequest(Long userId, String email, String subject, String message) {}
}
