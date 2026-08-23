package com.ss.shopai.service;

import com.ss.shopai.dto.request.ProductComparisonRequest;
import com.ss.shopai.dto.response.ProductComparisonResponse;
import com.ss.shopai.dto.response.ProductResponse;
import com.ss.shopai.entity.Product;
import com.ss.shopai.exception.BadRequestException;
import com.ss.shopai.exception.ResourceNotFoundException;
import com.ss.shopai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds side-by-side product comparisons. Designed to be reusable by the future
 * Voice Agent's "Comparison" stage in the Intent -> Search -> Recommendation -> Comparison pipeline.
 */
@Service
@RequiredArgsConstructor
public class ComparisonService {

    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public ProductComparisonResponse compareProducts(ProductComparisonRequest request) {
        List<Product> products = request.getProductIds().stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id)))
                .toList();

        if (products.size() != request.getProductIds().size()) {
            throw new BadRequestException("One or more product ids are invalid");
        }

        List<ProductResponse> responses = products.stream().map(productService::mapToResponse).toList();

        Map<String, List<String>> matrix = new LinkedHashMap<>();
        matrix.put("Brand", responses.stream().map(p -> p.getBrand() == null ? "N/A" : p.getBrand()).toList());
        matrix.put("Category", responses.stream().map(p -> p.getCategoryName() == null ? "N/A" : p.getCategoryName()).toList());
        matrix.put("Price", responses.stream().map(p -> "₹" + p.getPrice()).toList());
        matrix.put("Discounted Price", responses.stream().map(p -> "₹" + p.getDiscountedPrice()).toList());
        matrix.put("Discount %", responses.stream().map(p -> p.getDiscountPercentage() + "%").toList());
        matrix.put("Average Rating", responses.stream().map(p -> String.valueOf(p.getAverageRating())).toList());
        matrix.put("Total Reviews", responses.stream().map(p -> String.valueOf(p.getTotalReviews())).toList());
        matrix.put("Stock Availability", responses.stream().map(p -> p.isInStock() ? "In Stock (" + p.getStockQuantity() + ")" : "Out of Stock").toList());
        matrix.put("Total Sold", responses.stream().map(p -> String.valueOf(p.getTotalSold())).toList());

        return ProductComparisonResponse.builder()
                .products(responses)
                .attributeMatrix(matrix)
                .build();
    }
}