package com.ss.shopai.service;

import com.ss.shopai.entity.OrderItem;
import com.ss.shopai.entity.Product;
import com.ss.shopai.dto.response.ProductResponse;
import com.ss.shopai.repository.OrderItemRepository;
import com.ss.shopai.repository.OrderRepository;
import com.ss.shopai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Powers both general and personalized recommendations. Designed to also be the
 * backend hook for the future Voice Agent pipeline (Intent -> Search -> Recommendation -> Comparison).
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public List<ProductResponse> getRelatedProducts(Long productId, int limit) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || product.getCategory() == null) {
            return List.of();
        }

        return productRepository.findRecommendedByCategory(
                        product.getCategory().getId(), productId, PageRequest.of(0, limit))
                .stream()
                .map(productService::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getPersonalizedRecommendations(Long userId, int limit) {
        List<OrderItem> pastItems = orderRepository.findDeliveredOrderItemsByUser(userId);

        Set<Long> purchasedCategoryIds = pastItems.stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getCategory() != null)
                .map(item -> item.getProduct().getCategory().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> purchasedProductIds = pastItems.stream()
                .filter(item -> item.getProduct() != null)
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toSet());

        if (purchasedCategoryIds.isEmpty()) {
            // New user with no order history: fall back to top-rated products
            return getTopRated(limit);
        }

        List<ProductResponse> recommendations = new java.util.ArrayList<>();
        for (Long categoryId : purchasedCategoryIds) {
            if (recommendations.size() >= limit) {
                break;
            }
            productRepository.findRecommendedByCategory(categoryId, -1L, PageRequest.of(0, limit))
                    .stream()
                    .filter(p -> !purchasedProductIds.contains(p.getId()))
                    .limit(limit - recommendations.size())
                    .forEach(p -> recommendations.add(productService.mapToResponse(p)));
        }

        if (recommendations.size() < limit) {
            List<ProductResponse> fallback = getTopRated(limit - recommendations.size());
            Set<Long> existingIds = recommendations.stream().map(ProductResponse::getId).collect(Collectors.toSet());
            fallback.stream().filter(p -> !existingIds.contains(p.getId())).forEach(recommendations::add);
        }

        return recommendations;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getTopRated(int limit) {
        return productRepository.findTopRated(PageRequest.of(0, limit))
                .stream().map(productService::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getTopSelling(int limit) {
        return productRepository.findTopSelling(PageRequest.of(0, limit))
                .stream().map(productService::mapToResponse).toList();
    }
}