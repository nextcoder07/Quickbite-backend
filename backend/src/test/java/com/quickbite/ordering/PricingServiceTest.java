package com.quickbite.ordering;

import com.quickbite.shop.Product;
import com.quickbite.shop.Shop;
import com.quickbite.shop.ShopOwner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();
    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockProduct = mock(Product.class);
    }

    @Test
    @DisplayName("Should apply standard delivery fee when subtotal is below free threshold")
    void testStandardDeliveryFeeBelowThreshold() {
        OrderItem item1 = new OrderItem(mockProduct, "Burger", new BigDecimal("100.00"), 2); // 200.00

        PricingService.PricingCalculation calc = pricingService.calculatePricing(List.of(item1));

        assertEquals(new BigDecimal("200.00"), calc.subtotal());
        assertEquals(new BigDecimal("30.00"), calc.deliveryFee());
        assertEquals(new BigDecimal("0.00"), calc.discountAmount());
        assertEquals(new BigDecimal("230.00"), calc.totalAmount());
    }

    @Test
    @DisplayName("Should waive delivery fee when subtotal meets or exceeds free threshold")
    void testFreeDeliveryFeeAboveThreshold() {
        OrderItem item1 = new OrderItem(mockProduct, "Biryani Box", new BigDecimal("180.00"), 2); // 360.00

        PricingService.PricingCalculation calc = pricingService.calculatePricing(List.of(item1));

        assertEquals(new BigDecimal("360.00"), calc.subtotal());
        assertEquals(new BigDecimal("0.00"), calc.deliveryFee());
        assertEquals(new BigDecimal("360.00"), calc.totalAmount());
    }

    @Test
    @DisplayName("Should maintain invariant total = subtotal + fee - discount")
    void testInvariant() {
        OrderItem item1 = new OrderItem(mockProduct, "Item A", new BigDecimal("45.50"), 3);
        OrderItem item2 = new OrderItem(mockProduct, "Item B", new BigDecimal("12.25"), 2);

        PricingService.PricingCalculation calc = pricingService.calculatePricing(List.of(item1, item2));

        BigDecimal expectedTotal = calc.subtotal().add(calc.deliveryFee()).subtract(calc.discountAmount());
        assertEquals(expectedTotal, calc.totalAmount());
    }
}
