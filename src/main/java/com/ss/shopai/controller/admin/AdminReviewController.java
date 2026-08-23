package com.ss.shopai.controller.admin;

import com.ss.shopai.dto.response.ApiResponse;
import com.ss.shopai.dto.response.PagedResponse;
import com.ss.shopai.dto.response.ReviewResponse;
import com.ss.shopai.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getAllReviews(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ReviewResponse> response = reviewService.getAllReviewsForAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReviewByAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }
}