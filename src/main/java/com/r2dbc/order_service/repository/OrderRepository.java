package com.r2dbc.order_service.repository;

import com.r2dbc.order_service.domain.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OrderRepository extends ReactiveCrudRepository<Order, String> {
}
