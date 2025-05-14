package com.r2dbc.order_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2dbc.order_service.domain.Order;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

@Service
public class KafkaConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumer.class);

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
                        LOG.info("Saving Order with ID {} into DB", order.getOrderId());
                        return entityTemplate.insert(Order.class).using(order);
                    } catch (JsonProcessingException e) {
                        LOG.error("Caught JsonProcessingException while deserializing Json string to Order object");
                        return Mono.empty();
                    }
                })
                .subscribe();
    }
}
