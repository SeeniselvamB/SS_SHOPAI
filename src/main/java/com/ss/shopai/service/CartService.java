package com.ss.shopai.service;

import com.ss.shopai.dto.request.AddToCartRequest;
import com.ss.shopai.dto.request.UpdateCartItemRequest;
import com.ss.shopai.dto.response.CartItemResponse;
import com.ss.shopai.dto.response.CartResponse;
import com.ss.shopai.dto.response.ProductSummaryResponse;
import com.ss.shopai.entity.Cart;
import com.ss.shopai.entity.CartItem;
import com.ss.shopai.entity.Product;
import com.ss.shopai.entity.User;
import com.ss.shopai.exception.BadRequestException;
import com.ss.shopai.exception.ResourceNotFoundException;
import com.ss.shopai.repository.CartItemRepository;
import com.ss.shopai.repository.CartRepository;
import com.ss.shopai.repository.ProductRepository;
import com.ss.shopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            Cart cart = Cart.builder().user(user).build();
            return cartRepository.save(cart);
        });
    }

    @Transactional
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("This product is no longer available");
        }

        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()).orElse(null);

        int desiredQuantity = (existingItem != null ? existingItem.getQuantity() : 0) + request.getQuantity();
        if (desiredQuantity > product.getStockQuantity()) {
            throw new BadRequestException("Only " + product.getStockQuantity() + " units of \"" + product.getName() + "\" are available");
        }

        if (existingItem != null) {
            existingItem.setQuantity(desiredQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(item);
        }

        return getCart(userId);
    }

    @Transactional
    public CartResponse updateItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("This cart item does not belong to you");
        }

        if (request.getQuantity() > item.getProduct().getStockQuantity()) {
            throw new BadRequestException("Only " + item.getProduct().getStockQuantity() + " units available");
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return getCart(userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("This cart item does not belong to you");
        }

        cartItemRepository.delete(item);
        return getCart(userId);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        List<CartItemResponse> itemResponses = items.stream().map(this::mapToItemResponse).toList();

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .subtotal(subtotal)
                .totalItems(totalItems)
                .build();
    }

    private CartItemResponse mapToItemResponse(CartItem item) {
        Product product = item.getProduct();
        BigDecimal lineTotal = product.getDiscountedPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

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

        return CartItemResponse.builder()
                .id(item.getId())
                .product(summary)
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .build();
    }
}