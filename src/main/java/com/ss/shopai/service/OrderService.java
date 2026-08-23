package com.ss.shopai.service;

import com.ss.shopai.dto.request.CancelOrderRequest;
import com.ss.shopai.dto.request.CreateOrderRequest;
import com.ss.shopai.dto.request.UpdateOrderStatusRequest;
import com.ss.shopai.dto.response.OrderItemResponse;
import com.ss.shopai.dto.response.OrderResponse;
import com.ss.shopai.dto.response.PagedResponse;
import com.ss.shopai.dto.response.PaymentResponse;
import com.ss.shopai.entity.*;
import com.ss.shopai.enums.OrderStatus;
import com.ss.shopai.enums.PaymentMethod;
import com.ss.shopai.enums.PaymentStatus;
import com.ss.shopai.exception.BadRequestException;
import com.ss.shopai.exception.ResourceNotFoundException;
import com.ss.shopai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal FLAT_SHIPPING_FEE = new BigDecimal("49.00");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("999.00");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.05");
    private static final Set<OrderStatus> CANCELLABLE_STATUSES =
            Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AddressService addressService;
    private final CartService cartService;

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Address address = addressService.getAddressEntity(userId, request.getAddressId());

        // Validate stock for all items before committing anything
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (!product.isActive()) {
                throw new BadRequestException("\"" + product.getName() + "\" is no longer available");
            }
            if (cartItem.getQuantity() > product.getStockQuantity()) {
                throw new BadRequestException("Insufficient stock for \"" + product.getName() + "\". Only "
                        + product.getStockQuantity() + " left.");
            }
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .recipientName(address.getFullName())
                .recipientPhone(address.getPhoneNumber())
                .shippingAddressLine1(address.getAddressLine1())
                .shippingAddressLine2(address.getAddressLine2())
                .shippingCity(address.getCity())
                .shippingState(address.getState())
                .shippingPostalCode(address.getPostalCode())
                .shippingCountry(address.getCountry())
                .orderStatus(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .expectedDeliveryDate(LocalDateTime.now().plusDays(5))
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            BigDecimal lineSubtotal = product.getDiscountedPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(lineSubtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productImageUrl(product.getThumbnailUrl())
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(product.getPrice())
                    .discountAtPurchase(product.getDiscountPercentage())
                    .subtotal(lineSubtotal)
                    .build();

            order.getOrderItems().add(orderItem);

            // Decrement stock and bump sold counter
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            product.setTotalSold(product.getTotalSold() + cartItem.getQuantity());
            productRepository.save(product);
        }

        BigDecimal shippingFee = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : FLAT_SHIPPING_FEE;
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(shippingFee).add(taxAmount);

        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(shippingFee);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));

        Order savedOrder = orderRepository.save(order);

        // Clear cart after successful order placement
        cartService.clearCart(userId);

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        Page<Order> page = orderRepository.findByUserIdOrderByOrderDateDesc(userId, pageable);
        return PagedResponse.from(page.map(this::mapToResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrdersForAdmin(OrderStatus status, Pageable pageable) {
        Page<Order> page = status != null
                ? orderRepository.findByOrderStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return PagedResponse.from(page.map(this::mapToResponse));
    }

    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!CANCELLABLE_STATUSES.contains(order.getOrderStatus())) {
            throw new BadRequestException("Order cannot be cancelled once it is " + order.getOrderStatus());
        }

        restockOrderItems(order);

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledDate(LocalDateTime.now());
        order.setCancelReason(request.getReason());

        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot change status of an order that is already " + order.getOrderStatus());
        }

        if (request.getOrderStatus() == OrderStatus.CANCELLED) {
            restockOrderItems(order);
            order.setCancelledDate(LocalDateTime.now());
            order.setCancelReason("Cancelled by admin");
        }

        if (request.getOrderStatus() == OrderStatus.DELIVERED) {
            order.setDeliveredDate(LocalDateTime.now());
        }

        order.setOrderStatus(request.getOrderStatus());

        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            order.setTrackingNumber(request.getTrackingNumber());
        }

        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    private void restockOrderItems(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProduct() != null) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                product.setTotalSold(Math.max(0, product.getTotalSold() - item.getQuantity()));
                productRepository.save(product);
            }
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream().map(item ->
                OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductName())
                        .productImageUrl(item.getProductImageUrl())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .discountAtPurchase(item.getDiscountAtPurchase())
                        .subtotal(item.getSubtotal())
                        .build()
        ).toList();

        PaymentResponse paymentResponse = null;
        if (order.getPayment() != null) {
            Payment payment = order.getPayment();
            paymentResponse = PaymentResponse.builder()
                    .id(payment.getId())
                    .paymentMethod(payment.getPaymentMethod())
                    .paymentStatus(payment.getPaymentStatus())
                    .transactionId(payment.getTransactionId())
                    .amount(payment.getAmount())
                    .paymentDate(payment.getPaymentDate())
                    .failureReason(payment.getFailureReason())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .userFullName(order.getUser().getFullName())
                .orderItems(itemResponses)
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddressLine1(order.getShippingAddressLine1())
                .shippingAddressLine2(order.getShippingAddressLine2())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingPostalCode(order.getShippingPostalCode())
                .shippingCountry(order.getShippingCountry())
                .orderStatus(order.getOrderStatus())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .payment(paymentResponse)
                .orderDate(order.getOrderDate())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .deliveredDate(order.getDeliveredDate())
                .cancelledDate(order.getCancelledDate())
                .cancelReason(order.getCancelReason())
                .trackingNumber(order.getTrackingNumber())
                .build();
    }
}