package com.ss.shopai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Bound from query params for GET /api/products with search/filter/sort/pagination.
 * Kept flat (not @Valid'd) since all fields are optional query params.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {

    private String keyword;

    private Long categoryId;

    private String brand;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Double minRating;

    private Boolean inStockOnly;

    /** name | price | rating | newest | popularity */
    @Builder.Default
    private String sortBy = "newest";

    /** asc | desc */
    @Builder.Default
    private String sortDirection = "desc";

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 12;
}