package com.ss.shopai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private long totalCategories;
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private long pendingOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long lowStockProductCount;
    private long totalStockUnits;
    private Map<String, Long> ordersByStatus;
    private List<ProductSummaryResponse> topSellingProducts;
    private List<ProductSummaryResponse> lowStockProducts;
}