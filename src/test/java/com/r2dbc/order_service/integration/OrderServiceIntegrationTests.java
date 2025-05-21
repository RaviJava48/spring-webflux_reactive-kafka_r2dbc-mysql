package com.r2dbc.order_service.integration;

import com.r2dbc.order_service.domain.Order;
import com.r2dbc.order_service.repository.OrderRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
public class OrderServiceIntegrationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    private static final String BASE_URL = "/api/v1/orders";

    @BeforeEach
    void setUp() {
        Order order1 = new Order("AC6DE04BADE9", "Kiran", BigDecimal.valueOf(186.47), LocalDate.parse("2025-05-12"));
        Order order2 = new Order("F0161320FAD1", "Sai", BigDecimal.valueOf(236.09), LocalDate.parse("2025-05-11"));
        Order order3 = new Order("7D2FCF2B0945", "Sai", BigDecimal.valueOf(276.15), LocalDate.parse("2025-05-10"));

        Flux.just(order1, order2, order3)
                .flatMap(order -> entityTemplate.insert(Order.class).using(order))
                .then()
                .block();
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll().block();
    }

    @Test
    void createOrderTest() {

        Order order = new Order(null, "Mohan", BigDecimal.valueOf(336.17), LocalDate.parse("2025-05-12"));

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
                .uri(BASE_URL)
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
                .uri(BASE_URL + "/{id}", "7D2FCF2B0945")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class)
                .consumeWith(orderEntityExchangeResult -> {
                    Order order = orderEntityExchangeResult.getResponseBody();
                    assert order != null;
                    assertEquals("Bhanu", order.getCustomerName());
                });
    }

    @Test
    void getOrdersByCustomerNameTest() {

        String customerName = "Sai";

        Function<UriBuilder, URI> uri = uriBuilder -> uriBuilder.path(BASE_URL).queryParam("name", customerName).build();

        webTestClient.get()
                .uri(uri)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Order.class)
                .consumeWith(listEntityExchangeResult -> {
                    List<Order> orders = listEntityExchangeResult.getResponseBody();
                    assert orders != null;
                    assertEquals(2, orders.size());
                });
    }
}
