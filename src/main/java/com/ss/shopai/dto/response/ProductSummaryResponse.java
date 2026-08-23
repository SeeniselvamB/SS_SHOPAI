package com.ss.shopai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Lightweight product shape used in carts, wishlists, recommendations, order items.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {

    private Long id;
    private String name;
    private String thumbnailUrl;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Integer stockQuantity;
    private boolean inStock;
    private Double averageRating;
}