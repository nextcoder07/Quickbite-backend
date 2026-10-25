package com.quickbite.ordering;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PricingService {

    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("300.00");
    private static final BigDecimal STANDARD_DELIVERY_FEE = new BigDecimal("30.00");

    public record PricingCalculation(
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal discountAmount,
            BigDecimal totalAmount
    ) {}

    public PricingCalculation calculatePricing(List<OrderItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItem item : items) {
            BigDecimal line = item.getUnitPriceSnapshot()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(line);
        }

        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (subtotal.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(FREE_DELIVERY_THRESHOLD) < 0) {
            deliveryFee = STANDARD_DELIVERY_FEE;
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(deliveryFee).subtract(discountAmount);

        return new PricingCalculation(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                deliveryFee.setScale(2, RoundingMode.HALF_UP),
                discountAmount.setScale(2, RoundingMode.HALF_UP),
                totalAmount.setScale(2, RoundingMode.HALF_UP)
        );
    }
}
