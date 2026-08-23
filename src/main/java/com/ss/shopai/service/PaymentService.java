package com.ss.shopai.service;

import com.ss.shopai.dto.request.MockPaymentRequest;
import com.ss.shopai.dto.response.PaymentResponse;
import com.ss.shopai.entity.Order;
import com.ss.shopai.entity.Payment;
import com.ss.shopai.enums.OrderStatus;
import com.ss.shopai.enums.PaymentStatus;
import com.ss.shopai.exception.BadRequestException;
import com.ss.shopai.exception.ResourceNotFoundException;
import com.ss.shopai.repository.OrderRepository;
import com.ss.shopai.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentResponse processMockPayment(Long userId, MockPaymentRequest request) {
        Order order = orderRepository.findByIdAndUserId(request.getOrderId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled order");
        }

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("This order has already been paid for");
        }

        boolean simulateSuccess = request.getSimulateSuccess() == null || request.getSimulateSuccess();

        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .paymentMethod(order.getPayment() != null ? order.getPayment().getPaymentMethod() : com.ss.shopai.enums.PaymentMethod.UPI)
                    .amount(order.getTotalAmount())
                    .build();
        }

        if (simulateSuccess) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
            payment.setPaymentDate(LocalDateTime.now());
            payment.setFailureReason(null);
            order.setOrderStatus(OrderStatus.CONFIRMED);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment declined by gateway (simulated failure)");
            payment.setPaymentDate(LocalDateTime.now());
        }

        Payment savedPayment = paymentRepository.save(payment);
        order.setPayment(savedPayment);
        orderRepository.save(order);

        if (!simulateSuccess) {
            throw new BadRequestException("Payment failed: " + payment.getFailureReason());
        }

        return mapToResponse(savedPayment);
    }

    @Transactional
    public Payment initializePayment(Order order, com.ss.shopai.enums.PaymentMethod method) {
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(method)
                .paymentStatus(PaymentStatus.PENDING)
                .amount(order.getTotalAmount())
                .build();
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for order id: " + orderId));
        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .failureReason(payment.getFailureReason())
                .build();
    }
}