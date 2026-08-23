package com.ss.shopai.controller;

import com.ss.shopai.dto.request.ReviewRequest;
import com.ss.shopai.dto.response.ApiResponse;
import com.ss.shopai.dto.response.PagedResponse;
import com.ss.shopai.dto.response.ReviewResponse;
import com.ss.shopai.security.CurrentUser;
import com.ss.shopai.security.UserPrincipal;
import com.ss.shopai.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @PageableDefault(size = 10) Pageable pageable) {
        PagedResponse<ReviewResponse> response = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/product/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(@CurrentUser UserPrincipal principal,
                                                                   @PathVariable Long productId,
                                                                   @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.addReview(principal.getId(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Review added", response));
    }

    @PutMapping("/product/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(@CurrentUser UserPrincipal principal,
                                                                      @PathVariable Long productId,
                                                                      @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.updateReview(principal.getId(), productId, request);
        return ResponseEntity.ok(ApiResponse.success("Review updated", response));
    }

    @DeleteMapping("/product/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@CurrentUser UserPrincipal principal,
                                                            @PathVariable Long productId) {
        reviewService.deleteReview(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }
}