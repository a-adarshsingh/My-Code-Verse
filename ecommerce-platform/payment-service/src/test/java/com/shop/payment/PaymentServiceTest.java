package com.shop.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentServiceTest {

    private final PaymentService service = new PaymentService(null, new BigDecimal("1000"));

    @Test
    void approvesAtOrBelowLimit() {
        assertTrue(service.approve(new BigDecimal("1000")));
    }

    @Test
    void declinesAboveLimit() {
        assertFalse(service.approve(new BigDecimal("1000.01")));
    }
}
