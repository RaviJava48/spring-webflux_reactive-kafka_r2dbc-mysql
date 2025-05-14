package com.r2dbc.order_service.service;

import com.r2dbc.order_service.domain.Order;
import com.r2dbc.order_service.kafka.KafkaProducer;
import com.r2dbc.order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class OrderService {

    private final KafkaProducer kafkaProducer;
    private final OrderRepository orderRepository;

    public OrderService(KafkaProducer kafkaProducer, OrderRepository orderRepository) {
        this.kafkaProducer = kafkaProducer;
        this.orderRepository = orderRepository;
    }

    public Mono<Void> createOrder(Order order) {
        UUID uuid = UUID.randomUUID();
        String orderId = uuid.toString().replace("-", "").substring(0, 12).toUpperCase();
        order.setOrderId(orderId);

        return kafkaProducer.sendOrder(order);
    }

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Mono<Order> getOrderById(String orderId) {
        return orderRepository.findById(orderId);
    }

    public Mono<Order> updateById(Order order, String orderId) {
        return orderRepository.findById(orderId)
                .flatMap(existingOrder -> {
                    existingOrder.setCustomerName(order.getCustomerName());
                    existingOrder.setTotalAmount(order.getTotalAmount());
                    existingOrder.setOrderDate(order.getOrderDate());

                    return orderRepository.save(existingOrder);
                });
    }

    public Mono<String> deleteById(String orderId) {
        return orderRepository.findById(orderId)
                .flatMap(order -> orderRepository.delete(order).then(Mono.just("Order deleted successfully")));

    }
}
