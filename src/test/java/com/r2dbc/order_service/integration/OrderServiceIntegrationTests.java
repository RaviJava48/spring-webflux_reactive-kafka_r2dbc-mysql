package com.r2dbc.order_service.integration;

import com.r2dbc.order_service.domain.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
public class OrderServiceIntegrationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    private static final String BASE_URL = "/api/v1/orders";

    @Test
    void createOrderTest() {

        Order order = new Order(null, "Ravi Kumar", BigDecimal.valueOf(240.67), LocalDate.parse("2025-05-08"));

        webTestClient.post()
                .uri(BASE_URL + "/create-order")
                .bodyValue(order)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .isEqualTo("Order sent to Kafka topic successfully!");
    }

    @Test
    void printRecords() {

        // To print records of orders table from R2DBC H2 database (for test profile)
        databaseClient.sql("SELECT * FROM orders")
                .fetch()
                .all()
                .doOnNext(row -> System.out.println("Row: " + row))
                .blockLast();
    }

    @Test
    void getAllOrdersTest() {

        webTestClient.get()
                .uri(BASE_URL + "/")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Order.class)
                .consumeWith(listEntityExchangeResult -> {
                    List<Order> orders = listEntityExchangeResult.getResponseBody();
                    assert orders != null;
                    assertEquals(3, orders.size());
                });
    }

    @Test
    void getOrderByIdTest() {
        webTestClient.get()
                .uri(BASE_URL + "/{id}", "BB04D24C3FC1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class)
                .consumeWith(orderEntityExchangeResult -> {
                    Order order = orderEntityExchangeResult.getResponseBody();
                    assert order != null;
                    assertEquals("Ravi Kumar", order.getCustomerName());
                });
    }
}
