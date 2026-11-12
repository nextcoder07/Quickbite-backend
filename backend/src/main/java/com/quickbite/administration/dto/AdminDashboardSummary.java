package com.quickbite.administration.dto;

import java.math.BigDecimal;

public record AdminDashboardSummary(
        long totalUsers,
        long totalDeliveryPartners,
        long totalShops,
        long totalOrders,
        BigDecimal totalVolume,
        long pendingShopsCount,
        long pendingDeliveryPartnersCount,
        long pendingDeletionCount,
        long openDisputesCount,
        long pendingAdminRequestsCount
) {}
