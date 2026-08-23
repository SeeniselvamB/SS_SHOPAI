package com.ss.shopai.controller;

import com.ss.shopai.dto.request.AddToCartRequest;
import com.ss.shopai.dto.request.UpdateCartItemRequest;
import com.ss.shopai.dto.response.ApiResponse;
import com.ss.shopai.dto.response.CartResponse;
import com.ss.shopai.security.CurrentUser;
import com.ss.shopai.security.UserPrincipal;
import com.ss.shopai.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@CurrentUser UserPrincipal principal) {
        CartResponse response = cartService.getCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@CurrentUser UserPrincipal principal,
                                                               @Valid @RequestBody AddToCartRequest request) {
        CartResponse response = cartService.addItem(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", response));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(@CurrentUser UserPrincipal principal,
                                                                  @PathVariable Long cartItemId,
                                                                  @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse response = cartService.updateItem(principal.getId(), cartItemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", response));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@CurrentUser UserPrincipal principal,
                                                                  @PathVariable Long cartItemId) {
        CartResponse response = cartService.removeItem(principal.getId(), cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(@CurrentUser UserPrincipal principal) {
        cartService.clearCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}