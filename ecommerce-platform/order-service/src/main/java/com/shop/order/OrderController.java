package com.shop.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** Checks out the caller's current cart. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order checkout(@RequestHeader("X-User-Id") Long userId,
                          @RequestHeader(value = "X-User-Email", required = false) String email) {
        return service.checkout(userId, email);
    }

    @GetMapping
    public List<Order> mine(@RequestHeader("X-User-Id") Long userId) {
        return service.myOrders(userId);
    }

    @GetMapping("/{id}")
    public Order one(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return service.myOrder(userId, id);
    }
}
