package com.ss.shopai.service;

import com.ss.shopai.dto.request.ReviewRequest;
import com.ss.shopai.dto.response.PagedResponse;
import com.ss.shopai.dto.response.ReviewResponse;
import com.ss.shopai.entity.Product;
import com.ss.shopai.entity.Review;
import com.ss.shopai.entity.User;
import com.ss.shopai.exception.BadRequestException;
import com.ss.shopai.exception.ResourceNotFoundException;
import com.ss.shopai.repository.OrderItemRepository;
import com.ss.shopai.repository.ProductRepository;
import com.ss.shopai.repository.ReviewRepository;
import com.ss.shopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public ReviewResponse addReview(Long userId, Long productId, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new BadRequestException("You have already reviewed this product. Please edit your existing review.");
        }

        boolean verifiedPurchase = orderItemRepository.existsByOrder_User_IdAndProduct_Id(userId, productId);

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .verifiedPurchase(verifiedPurchase)
                .build();

        Review saved = reviewRepository.save(review);
        recalculateProductRating(product);

        return mapToResponse(saved);
    }

    @Transactional
    public ReviewResponse updateReview(Long userId, Long productId, ReviewRequest request) {
        Review review = reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for this product"));

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        Review saved = reviewRepository.save(review);

        recalculateProductRating(review.getProduct());

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteReview(Long userId, Long productId) {
        Review review = reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for this product"));

        Product product = review.getProduct();
        reviewRepository.delete(review);
        recalculateProductRating(product);
    }

    @Transactional
    public void deleteReviewByAdmin(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        Product product = review.getProduct();
        reviewRepository.delete(review);
        recalculateProductRating(product);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        return PagedResponse.from(page.map(this::mapToResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getAllReviewsForAdmin(Pageable pageable) {
        Page<Review> page = reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PagedResponse.from(page.map(this::mapToResponse));
    }

    private void recalculateProductRating(Product product) {
        List<Review> reviews = reviewRepository.findByProductId(product.getId());
        int totalReviews = reviews.size();
        double averageRating = totalReviews == 0 ? 0.0 :
                reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);

        product.setTotalReviews(totalReviews);
        product.setAverageRating(Math.round(averageRating * 10.0) / 10.0);
        productRepository.save(product);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .userId(review.getUser().getId())
                .userFullName(review.getUser().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.isVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .build();
    }
}