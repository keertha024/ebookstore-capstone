package com.ebookstore.backend.service;

import com.ebookstore.backend.dto.OrderItemResponse;
import com.ebookstore.backend.dto.OrderResponse;
import com.ebookstore.backend.entity.*;
import com.ebookstore.backend.repository.CartRepository;
import com.ebookstore.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public OrderResponse checkout(String deliveryAddress) {
        User user = currentUserService.getCurrentUser();

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot checkout an empty cart");
        }

        Order order = new Order();
        order.setUser(user);
        order.setDeliveryAddress(deliveryAddress);
        order.setStatus(Order.OrderStatus.PENDING);

        double total = 0.0;
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getProduct().getPrice()); // snapshot price at purchase time
            order.getItems().add(orderItem);
            total += cartItem.getProduct().getPrice() * cartItem.getQuantity();
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        // Clear the cart after a successful checkout
        cart.getItems().clear();
        cartRepository.save(cart);

        return toResponse(saved);
    }

    @Transactional
    public List<OrderResponse> getOrderHistory() {
        User user = currentUserService.getCurrentUser();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse getOrderById(Long id) {
        User user = currentUserService.getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        if (!order.getUser().getId().equals(user.getId())) {
            // Don't reveal that the order exists for someone else
            throw new IllegalArgumentException("Order not found: " + id);
        }

        return toResponse(order);
    }

    @Transactional
    public void reorder(Long id) {
        User user = currentUserService.getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Order not found: " + id);
        }

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        for (OrderItem orderItem : order.getItems()) {
            CartItem existing = cart.getItems().stream()
                    .filter(ci -> ci.getProduct().getId().equals(orderItem.getProduct().getId()))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + orderItem.getQuantity());
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setProduct(orderItem.getProduct());
                newItem.setQuantity(orderItem.getQuantity());
                cart.getItems().add(newItem);
            }
        }

        cartRepository.save(cart);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                itemResponses,
                order.getTotalAmount(),
                order.getDeliveryAddress(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }
}
