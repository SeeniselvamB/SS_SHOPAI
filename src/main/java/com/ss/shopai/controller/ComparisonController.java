package com.ss.shopai.controller;

import com.ss.shopai.dto.request.ProductComparisonRequest;
import com.ss.shopai.dto.response.ApiResponse;
import com.ss.shopai.dto.response.ProductComparisonResponse;
import com.ss.shopai.service.ComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Comparison endpoint. Also the seam the future Voice Agent's "Comparison" stage will call
 * after Intent -> Search -> Recommendation resolve a candidate product set.
 */
@RestController
@RequestMapping("/api/comparison")
@RequiredArgsConstructor
public class ComparisonController {

    private final ComparisonService comparisonService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductComparisonResponse>> compareProducts(
            @Valid @RequestBody ProductComparisonRequest request) {
        ProductComparisonResponse response = comparisonService.compareProducts(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}