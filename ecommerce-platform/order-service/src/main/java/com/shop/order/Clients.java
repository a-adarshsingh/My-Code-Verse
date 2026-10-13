package com.shop.order;

import com.shop.order.Dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Thin REST clients for the services the order flow depends on. */
@Component
public class Clients {

    private static final Logger log = LoggerFactory.getLogger(Clients.class);

    private final RestClient cart;
    private final RestClient product;
    private final RestClient payment;
    private final RestClient notification;

    public Clients(RestClient.Builder builder) {
        this.cart = builder.clone().baseUrl("http://cart-service").build();
        this.product = builder.clone().baseUrl("http://product-service").build();
        this.payment = builder.clone().baseUrl("http://payment-service").build();
        this.notification = builder.clone().baseUrl("http://notification-service").build();
    }

    public List<CartItemDto> getCart(Long userId) {
        return cart.get().uri("/internal/cart/{id}", userId).retrieve()
                .body(new ParameterizedTypeReference<List<CartItemDto>>() {});
    }

    public void clearCart(Long userId) {
        cart.delete().uri("/internal/cart/{id}", userId).retrieve().toBodilessEntity();
    }

    public ProductDto getProduct(Long id) {
        try {
            return product.get().uri("/api/products/{id}", id).retrieve().body(ProductDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product " + id + " no longer exists");
        }
    }

    /** @return true if stock was reserved, false if there was not enough stock. */
    public boolean reserve(Long productId, int quantity) {
        try {
            product.post().uri("/internal/products/{id}/reserve?quantity={q}", productId, quantity)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.Conflict e) {
            return false;
        }
    }

    public void release(Long productId, int quantity) {
        try {
            product.post().uri("/internal/products/{id}/release?quantity={q}", productId, quantity)
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Could not release stock for product {} (qty {}): needs manual reconciliation", productId, quantity, e);
        }
    }

    public PaymentResult pay(PaymentRequest request) {
        return payment.post().uri("/internal/payments").body(request).retrieve().body(PaymentResult.class);
    }

    /** Best effort: a failed notification must never fail an order. */
    public void notify(NotificationRequest request) {
        try {
            notification.post().uri("/internal/notifications").body(request).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Notification failed: {}", e.getMessage());
        }
    }
}
