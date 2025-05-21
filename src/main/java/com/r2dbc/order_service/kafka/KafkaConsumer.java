package com.r2dbc.order_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2dbc.order_service.domain.Order;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

@Service
public class KafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);

    private final ReceiverOptions<String, String> receiverOptions;
    private final ObjectMapper objectMapper;
    private final R2dbcEntityTemplate entityTemplate;

    public KafkaConsumer(ReceiverOptions<String, String> receiverOptions, ObjectMapper objectMapper, R2dbcEntityTemplate entityTemplate) {
        this.receiverOptions = receiverOptions;
        this.objectMapper = objectMapper;
        this.entityTemplate = entityTemplate;
    }

    @PostConstruct
    public void consumeMessages() {
        KafkaReceiver.create(receiverOptions)
                .receive()
                .flatMap(record -> {
                    try {
                        Order order = objectMapper.readValue(record.value(), Order.class);
                        LOGGER.info("Saving Order with ID {} into DB", order.getOrderId());
                        return entityTemplate.insert(Order.class).using(order)
                                .map(savedOrder -> "Order placed with ID " + savedOrder.getOrderId())
                                .onErrorResume(ex -> {
                                    LOGGER.error("Exception thrown: {}", ex.getClass().getName());
                                    if (ex instanceof BadSqlGrammarException) {
                                        LOGGER.error("Bad SQL Grammar: {}", ex.getMessage());
                                        return Mono.just("Failed to save Order due to bad SQL grammar");
                                    } else if (ex instanceof DataIntegrityViolationException) {
                                        LOGGER.error("Data Integrity Violation: {}", ex.getMessage());
                                        return Mono.just("Order couldn't be saved due to required field missing or null");
                                    } else {
                                        LOGGER.error("Unexpected error occurred: {}", ex.getMessage());
                                        return Mono.just("An unexpected error occurred while saving the order");
                                    }
                                });
                    } catch (JsonProcessingException e) {
                        LOGGER.error("Caught JsonProcessingException while deserializing Json string to Order object");
                        return Mono.empty();
                    }
                })
                .subscribe();
    }
}
