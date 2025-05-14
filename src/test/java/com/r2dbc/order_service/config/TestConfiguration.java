package com.r2dbc.order_service.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.r2dbc.core.DatabaseClient;

@org.springframework.boot.test.context.TestConfiguration
public class TestConfiguration {

    @Bean
    public ApplicationListener<ApplicationReadyEvent> initDb(DatabaseClient databaseClient) {
        return event -> {
            databaseClient.sql("""
                CREATE TABLE orders (
                    order_id VARCHAR(255) PRIMARY KEY,
                    customer_name VARCHAR(255) NOT NULL,
                    total_amount DECIMAL(10, 2) NOT NULL,
                    order_date DATE NOT NULL
                )
            """).then()
            .subscribe();
        };
    }
}
