package com.ss.shopai.controller;

import com.ss.shopai.dto.request.CancelOrderRequest;
import com.ss.shopai.dto.request.CreateOrderRequest;
import com.ss.shopai.dto.request.MockPaymentRequest;
import com.ss.shopai.dto.response.ApiResponse;
import com.ss.shopai.dto.response.OrderResponse;
import com.ss.shopai.dto.response.PagedResponse;
import com.ss.shopai.dto.response.PaymentResponse;
import com.ss.shopai.security.CurrentUser;
import com.ss.shopai.security.UserPrincipal;
import com.ss.shopai.service.OrderService;
import com.ss.shopai.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@CurrentUser UserPrincipal principal,
                                                                    @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getUserOrders(
            @CurrentUser UserPrincipal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        PagedResponse<OrderResponse> response = orderService.getUserOrders(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@CurrentUser UserPrincipal principal,
                                                                     @PathVariable Long orderId) {
        OrderResponse response = orderService.getOrderById(principal.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@CurrentUser UserPrincipal principal,
                                                                    @PathVariable Long orderId,
                                                                    @Valid @RequestBody CancelOrderRequest request) {
        OrderResponse response = orderService.cancelOrder(principal.getId(), orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", response));
    }

    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@CurrentUser UserPrincipal principal,
                                                                         @Valid @RequestBody MockPaymentRequest request) {
        PaymentResponse response = paymentService.processMockPayment(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", response));
    }
}