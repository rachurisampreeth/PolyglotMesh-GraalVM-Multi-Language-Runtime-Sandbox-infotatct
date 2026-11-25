package com.polyglotmesh;

import com.polyglotmesh.service.PricingMath;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PricingMathTest {
    @Test void calculateAppliesDiscount() { assertEquals(2700, PricingMath.calculate(1500, 2, 10), 0.001); }
    @Test void rejectsInvalidQuantity() { assertThrows(IllegalArgumentException.class, () -> PricingMath.calculate(10, 0, 0)); }
}
