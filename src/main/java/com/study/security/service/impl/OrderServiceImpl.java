package com.study.security.service.impl;

import com.study.security.enums.OrderStatus;
import com.study.security.exception.ResourceNotFoundException;
import com.study.security.request.OrderRequest;
import com.study.security.request.OrderItemRequest;
import com.study.security.exception.BusinessException;
import com.study.security.model.Customer;
import com.study.security.model.Order;
import com.study.security.model.OrderItem;
import com.study.security.model.Product;
import com.study.security.repository.CustomerRepository;
import com.study.security.repository.OrderItemRepository;
import com.study.security.repository.OrderRepository;
import com.study.security.repository.ProductRepository;
import com.study.security.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
// Create a constructor with all required parameters (final)
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public Order save(OrderRequest orderRequest) {
        Customer customer = customerRepository.findById(orderRequest.getCustomerId())
                .orElseThrow(() -> new BusinessException("Customer not found."));

        Order order = Order.builder()
                .total(orderRequest.getTotal())
                .orderDate(LocalDate.now())
                .customer(customer)
                .status(OrderStatus.SUCCESS)
                .build();

        List<OrderItem> orderItems = this.processOrderItem(order, orderRequest.getItems());

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        order.setOrderItems(orderItems);
        return order;
    }

    public Optional<Order> getOrderInfo(Long id) {
        return this.orderRepository.findByIdFetchOrderItems(id);
    }

    @Transactional
    public Order updateStatus(Long id, OrderStatus status) {
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(status);
                    return orderRepository.save(order);
                }).orElseThrow(() -> new ResourceNotFoundException("Order not found."));
    }

    private List<OrderItem> processOrderItem(Order order, List<OrderItemRequest> items) {
        if (items.isEmpty()) {
            throw new BusinessException("To save an order is required at least one product");
        }
        return items.stream().map(dto -> {

            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new BusinessException("Invalid product code: " + dto.getProductId()));

            OrderItem orderItem = OrderItem.builder()
                    .quantity(dto.getQuantity())
                    .order(order)
                    .product(product)
                    .build();

            return orderItem;
        }).collect(Collectors.toList());
    }
}
