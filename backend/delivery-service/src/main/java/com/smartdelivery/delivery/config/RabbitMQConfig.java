package com.smartdelivery.delivery.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.created.name}")
    private String createdQueueName;

    // Création de l'exchange
    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(exchangeName);
    }

    // Création des queues
    @Bean
    public Queue createdQueue() {
        return QueueBuilder.durable(createdQueueName).build();
    }

    // Binding entre l'exchange et les queues
    @Bean
    public Binding createdBinding() {
        return BindingBuilder
                .bind(createdQueue())
                .to(deliveryExchange())
                .with("delivery.created");
    }
}
