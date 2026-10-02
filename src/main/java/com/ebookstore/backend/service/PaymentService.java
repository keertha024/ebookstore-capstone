package com.ebookstore.backend.service;

import com.ebookstore.backend.dto.PaymentRequest;
import com.ebookstore.backend.dto.PaymentResponse;
import com.ebookstore.backend.entity.Order;
import com.ebookstore.backend.entity.Payment;
import com.ebookstore.backend.entity.User;
import com.ebookstore.backend.repository.OrderRepository;
import com.ebookstore.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public PaymentResponse pay(PaymentRequest request) {
        User user = currentUserService.getCurrentUser();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Order not found: " + request.getOrderId());
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalArgumentException("Order is not in a payable state: " + order.getStatus());
        }

        // Simulated payment processing - in a real system this would call a payment gateway.
        // For this capstone, we treat any syntactically valid request as successful.
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(request.getMethod());
        payment.setStatus(Payment.PaymentStatus.SUCCESS);

        Payment saved = paymentRepository.save(payment);

        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);

        return toResponse(saved);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getMethod(),
                payment.getStatus().name(),
                payment.getTransactionDate()
        );
    }
}
