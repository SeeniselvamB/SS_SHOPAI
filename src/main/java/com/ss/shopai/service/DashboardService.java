package com.ss.shopai.service;

import com.ss.shopai.dto.response.DashboardStatsResponse;
import com.ss.shopai.dto.response.ProductSummaryResponse;
import com.ss.shopai.entity.Product;
import com.ss.shopai.enums.OrderStatus;
import com.ss.shopai.repository.CategoryRepository;
import com.ss.shopai.repository.OrderRepository;
import com.ss.shopai.repository.ProductRepository;
import com.ss.shopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates statistics for the Admin Dashboard: sales, inventory, and order breakdowns.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.countByActiveTrue();
        long totalOrders = orderRepository.count();
        long totalCategories = categoryRepository.count();

        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(23, 59, 59);
        BigDecimal todayRevenue = orderRepository.sumRevenueBetween(startOfToday, endOfToday);

        long pendingOrders = orderRepository.countByOrderStatus(OrderStatus.PENDING);
        long deliveredOrders = orderRepository.countByOrderStatus(OrderStatus.DELIVERED);
        long cancelledOrders = orderRepository.countByOrderStatus(OrderStatus.CANCELLED);

        List<Product> lowStock = productRepository.findLowStock(LOW_STOCK_THRESHOLD);
        long totalStockUnits = productRepository.sumStockQuantity();

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status.name(), orderRepository.countByOrderStatus(status));
        }

        List<ProductSummaryResponse> topSelling = productRepository.findTopSelling(PageRequest.of(0, 5))
                .stream().map(this::mapToSummary).toList();

        List<ProductSummaryResponse> lowStockSummaries = lowStock.stream()
                .map(this::mapToSummary).toList();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalCategories(totalCategories)
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .pendingOrders(pendingOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .lowStockProductCount(lowStock.size())
                .totalStockUnits(totalStockUnits)
                .ordersByStatus(ordersByStatus)
                .topSellingProducts(topSelling)
                .lowStockProducts(lowStockSummaries)
                .build();
    }

    private ProductSummaryResponse mapToSummary(Product product) {
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .thumbnailUrl(product.getThumbnailUrl())
                .price(product.getPrice())
                .discountedPrice(product.getDiscountedPrice())
                .stockQuantity(product.getStockQuantity())
                .inStock(product.getStockQuantity() > 0)
                .averageRating(product.getAverageRating())
                .build();
    }
}