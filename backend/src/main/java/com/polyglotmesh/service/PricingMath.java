package com.polyglotmesh.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PricingMath {
    private PricingMath() {}
    public static double calculate(double price, int quantity, double discountPercent) {
        if (quantity < 1 || quantity > 10000 || discountPercent < 0 || discountPercent > 100 || !Double.isFinite(price)) {
            throw new IllegalArgumentException("Invalid price, quantity or discount");
        }
        return BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity))
            .multiply(BigDecimal.valueOf(1 - discountPercent / 100))
            .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
