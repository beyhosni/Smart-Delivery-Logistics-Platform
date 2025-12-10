package com.smartdelivery.tracking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.dispatched.name}")
    private String dispatchedQueueName;

    @Value("${rabbitmq.queue.in_transit.name}")
    private String inTransitQueueName;

    // Création de l'exchange
    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(exchangeName);
    }

    // Création des queues
    @Bean
    public Queue dispatchedQueue() {
        return QueueBuilder.durable(dispatchedQueueName).build();
    }

    @Bean
    public Queue inTransitQueue() {
        return QueueBuilder.durable(inTransitQueueName).build();
    }

    // Binding entre l'exchange et les queues
    @Bean
    public Binding dispatchedBinding() {
        return BindingBuilder
                .bind(dispatchedQueue())
                .to(deliveryExchange())
                .with("delivery.dispatched");
    }

    @Bean
    public Binding inTransitBinding() {
        return BindingBuilder
                .bind(inTransitQueue())
                .to(deliveryExchange())
                .with("delivery.in_transit");
    }

    // Configuration du convertisseur de messages JSON
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Configuration du conteneur d'écoute RabbitMQ
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
