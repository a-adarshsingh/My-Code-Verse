package com.shop.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Mock payment gateway. Swap {@link #approve(BigDecimal)} for a real Stripe/PayPal call.
 * Amounts above payment.decline-above are declined so you can exercise the failure path.
 */
@Service
public class PaymentService {

    private final PaymentRepository repo;
    private final BigDecimal declineAbove;

    public PaymentService(PaymentRepository repo,
                          @Value("${payment.decline-above:100000}") BigDecimal declineAbove) {
        this.repo = repo;
        this.declineAbove = declineAbove;
    }

    public boolean approve(BigDecimal amount) {
        return amount.compareTo(declineAbove) <= 0;
    }

    public Payment charge(Long orderId, BigDecimal amount) {
        return repo.save(new Payment(orderId, amount, approve(amount) ? "SUCCESS" : "DECLINED"));
    }
}
