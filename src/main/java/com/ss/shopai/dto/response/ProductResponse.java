package com.ss.shopai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String sku;
    private String description;
    private String brand;
    private BigDecimal price;
    private BigDecimal discountPercentage;
    private BigDecimal discountedPrice;
    private Integer stockQuantity;
    private boolean inStock;
    private String thumbnailUrl;
    private Long categoryId;
    private String categoryName;
    private boolean active;
    private Double averageRating;
    private Integer totalReviews;
    private Integer totalSold;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}