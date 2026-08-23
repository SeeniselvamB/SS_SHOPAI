package com.ss.shopai.controller;

import com.ss.shopai.dto.response.ApiResponse;
import com.ss.shopai.dto.response.WishlistResponse;
import com.ss.shopai.security.CurrentUser;
import com.ss.shopai.security.UserPrincipal;
import com.ss.shopai.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(@CurrentUser UserPrincipal principal) {
        WishlistResponse response = wishlistService.getWishlist(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponse>> addItem(@CurrentUser UserPrincipal principal,
                                                                   @PathVariable Long productId) {
        WishlistResponse response = wishlistService.addItem(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Item added to wishlist", response));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponse>> removeItem(@CurrentUser UserPrincipal principal,
                                                                      @PathVariable Long productId) {
        WishlistResponse response = wishlistService.removeItem(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from wishlist", response));
    }
}