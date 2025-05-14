package com.r2dbc.order_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2dbc.order_service.domain.Order;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Service
public class KafkaProducer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducer.class);

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;

    public KafkaProducer(KafkaSender<String, String> kafkaSender, ObjectMapper objectMapper) {
        this.kafkaSender = kafkaSender;
        this.objectMapper = objectMapper;
    }

    @Value("${kafka.topic}")
    private String topicName;

    public Mono<Void> sendOrder(Order order) {
        try{
            String jsonString = objectMapper.writeValueAsString(order);
            SenderRecord<String, String, String> senderRecord
                    = SenderRecord.create(new ProducerRecord<>(topicName, order.getOrderId(), jsonString), order.getOrderId());
            LOG.info("Sending Order with ID {} into topic {}", order.getOrderId(), topicName);
            return kafkaSender.send(Mono.just(senderRecord)).then();
        } catch (JsonProcessingException e) {
            LOG.error("Caught JsonProcessingException while serializing Order object into Json string");
            return Mono.error(new RuntimeException("Serialization failed"));
        }
    }

}
