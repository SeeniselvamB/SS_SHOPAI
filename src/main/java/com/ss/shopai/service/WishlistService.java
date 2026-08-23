package com.ss.shopai.service;

import com.ss.shopai.dto.response.ProductSummaryResponse;
import com.ss.shopai.dto.response.WishlistItemResponse;
import com.ss.shopai.dto.response.WishlistResponse;
import com.ss.shopai.entity.Product;
import com.ss.shopai.entity.User;
import com.ss.shopai.entity.Wishlist;
import com.ss.shopai.entity.WishlistItem;
import com.ss.shopai.exception.BadRequestException;
import com.ss.shopai.exception.ResourceNotFoundException;
import com.ss.shopai.repository.ProductRepository;
import com.ss.shopai.repository.UserRepository;
import com.ss.shopai.repository.WishlistItemRepository;
import com.ss.shopai.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Wishlist getOrCreateWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            Wishlist wishlist = Wishlist.builder().user(user).build();
            return wishlistRepository.save(wishlist);
        });
    }

    @Transactional
    public WishlistResponse addItem(Long userId, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            throw new BadRequestException("This product is already in your wishlist");
        }

        WishlistItem item = WishlistItem.builder()
                .wishlist(wishlist)
                .product(product)
                .build();
        wishlistItemRepository.save(item);

        return getWishlist(userId);
    }

    @Transactional
    public WishlistResponse removeItem(Long userId, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        wishlistItemRepository.deleteByWishlistIdAndProductId(wishlist.getId(), productId);
        return getWishlist(userId);
    }

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long userId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        List<WishlistItem> items = wishlistItemRepository.findByWishlistId(wishlist.getId());

        List<WishlistItemResponse> itemResponses = items.stream().map(item -> {
            Product product = item.getProduct();
            ProductSummaryResponse summary = ProductSummaryResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .thumbnailUrl(product.getThumbnailUrl())
                    .price(product.getPrice())
                    .discountedPrice(product.getDiscountedPrice())
                    .stockQuantity(product.getStockQuantity())
                    .inStock(product.getStockQuantity() > 0)
                    .averageRating(product.getAverageRating())
                    .build();

            return WishlistItemResponse.builder()
                    .id(item.getId())
                    .product(summary)
                    .addedAt(item.getAddedAt())
                    .build();
        }).toList();

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .items(itemResponses)
                .build();
    }
}