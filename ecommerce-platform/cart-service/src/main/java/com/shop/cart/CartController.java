package com.shop.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CartController {

    public record AddItemRequest(@NotNull Long productId, @Min(1) int quantity) {}

    private final CartItemRepository repo;

    public CartController(CartItemRepository repo) {
        this.repo = repo;
    }

    // ---- public API (user id injected by the gateway) ----

    @GetMapping("/api/cart")
    public List<CartItem> myCart(@RequestHeader("X-User-Id") Long userId) {
        return repo.findByUserId(userId);
    }

    @PostMapping("/api/cart/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartItem add(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody AddItemRequest r) {
        CartItem item = repo.findByUserIdAndProductId(userId, r.productId())
                .orElseGet(() -> new CartItem(userId, r.productId(), 0));
        item.setQuantity(item.getQuantity() + r.quantity());
        return repo.save(item);
    }

    @DeleteMapping("/api/cart/items/{productId}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@RequestHeader("X-User-Id") Long userId, @PathVariable Long productId) {
        repo.deleteByUserIdAndProductId(userId, productId);
    }

    @DeleteMapping("/api/cart")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearMine(@RequestHeader("X-User-Id") Long userId) {
        repo.deleteByUserId(userId);
    }

    // ---- internal API used by order-service ----

    @GetMapping("/internal/cart/{userId}")
    public List<CartItem> cartOf(@PathVariable Long userId) {
        return repo.findByUserId(userId);
    }

    @DeleteMapping("/internal/cart/{userId}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable Long userId) {
        repo.deleteByUserId(userId);
    }
}
