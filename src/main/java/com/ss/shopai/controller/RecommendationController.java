package com.ss.shopai.controller;

import com.ss.shopai.dto.response.ApiResponse;
import com.ss.shopai.dto.response.ProductResponse;
import com.ss.shopai.security.CurrentUser;
import com.ss.shopai.security.UserPrincipal;
import com.ss.shopai.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Recommendation endpoints. This is also the seam the future Voice Agent's
 * "Recommendation" stage will call (Intent -> Search -> Recommendation -> Comparison).
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/related/{productId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getRelatedProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") int limit) {
        List<ProductResponse> response = recommendationService.getRelatedProducts(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getTopRated(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponse> response = recommendationService.getTopRated(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/top-selling")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getTopSelling(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponse> response = recommendationService.getTopSelling(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/personalized")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPersonalizedRecommendations(
            @CurrentUser UserPrincipal principal,
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponse> response = recommendationService.getPersonalizedRecommendations(principal.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}