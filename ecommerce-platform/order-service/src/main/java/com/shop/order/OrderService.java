package com.shop.order;

import com.shop.order.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Checkout orchestration (a simple saga with compensation):
 * 1. read cart  2. reserve stock per item  3. save PENDING order  4. charge payment
 * 5. on success: PAID + clear cart; on failure: FAILED + release stock  6. notify.
 */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final Clients clients;

    public OrderService(OrderRepository orders, Clients clients) {
        this.orders = orders;
        this.clients = clients;
    }

    public Order checkout(Long userId, String email) {
        List<CartItemDto> cart = clients.getCart(userId);
        if (cart.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);

        List<CartItemDto> reserved = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        try {
            for (CartItemDto line : cart) {
                ProductDto product = clients.getProduct(line.productId());
                if (!clients.reserve(product.id(), line.quantity())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for " + product.name());
                }
                reserved.add(line);
                order.getItems().add(new OrderItem(product.id(), product.name(), product.price(), line.quantity()));
                total = total.add(product.price().multiply(BigDecimal.valueOf(line.quantity())));
            }
        } catch (RuntimeException e) {
            releaseAll(reserved);
            throw e;
        }
        order.setTotal(total);
        order = orders.save(order);

        PaymentResult result;
        try {
            result = clients.pay(new PaymentRequest(order.getId(), total));
        } catch (RuntimeException e) {
            result = new PaymentResult("FAILED", null);
        }

        if ("SUCCESS".equals(result.status())) {
            order.setStatus(OrderStatus.PAID);
            clients.clearCart(userId);
        } else {
            order.setStatus(OrderStatus.FAILED);
            releaseAll(reserved);
        }
        order = orders.save(order);

        clients.notify(new NotificationRequest(userId, email,
                "Order #" + order.getId() + " " + order.getStatus(),
                "Your order total is " + order.getTotal() + " and its status is " + order.getStatus() + "."));
        return order;
    }

    public List<Order> myOrders(Long userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order myOrder(Long userId, Long orderId) {
        return orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private void releaseAll(List<CartItemDto> reserved) {
        reserved.forEach(r -> clients.release(r.productId(), r.quantity()));
    }
}
