package com.r2dbc.order_service.controller;

import com.r2dbc.order_service.domain.Order;
import com.r2dbc.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create-order")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<String> createOrder(@RequestBody @Valid Order order) {
        return orderService.createOrder(order)
                .thenReturn("Order sent to Kafka topic successfully!")
                .log();
    }

    @GetMapping()
    public Flux<Order> getAllOrders(@RequestParam(value = "name", required = false) String customerName) {
        if(!Objects.isNull(customerName)) {
            return orderService.getOrdersByCustomerName(customerName);
        }

        return orderService.getAllOrders().log();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Order>> getOrderById(@PathVariable("id") String orderId) {
        return orderService.getOrderById(orderId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .log();
    }

    @PutMapping("/update/{id}")
    public Mono<ResponseEntity<Order>> updateById(@RequestBody Order order, @PathVariable("id") String orderId) {
        return orderService.updateById(order, orderId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .log();
    }

    @DeleteMapping("/delete/{id}")
    public Mono<ResponseEntity<String>> deleteById(@PathVariable("id") String orderId) {
        return orderService.deleteById(orderId).log()
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order Not Found with ID " + orderId)))
                .log();
    }
}
