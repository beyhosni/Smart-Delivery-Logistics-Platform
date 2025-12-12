package com.smartdelivery.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

    @Value("${rabbitmq.queue.delivered.name}")
    private String deliveredQueueName;

    @Value("${rabbitmq.routingkey.dispatched}")
    private String dispatchedRoutingKey;

    @Value("${rabbitmq.routingkey.in_transit}")
    private String inTransitRoutingKey;

    @Value("${rabbitmq.routingkey.delivered}")
    private String deliveredRoutingKey;

    @Bean
    public Queue dispatchedQueue() {
        return new Queue(dispatchedQueueName, true);
    }

    @Bean
    public Queue inTransitQueue() {
        return new Queue(inTransitQueueName, true);
    }

    @Bean
    public Queue deliveredQueue() {
        return new Queue(deliveredQueueName, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Binding dispatchedBinding() {
        return BindingBuilder
                .bind(dispatchedQueue())
                .to(exchange())
                .with(dispatchedRoutingKey);
    }

    @Bean
    public Binding inTransitBinding() {
        return BindingBuilder
                .bind(inTransitQueue())
                .to(exchange())
                .with(inTransitRoutingKey);
    }

    @Bean
    public Binding deliveredBinding() {
        return BindingBuilder
                .bind(deliveredQueue())
                .to(exchange())
                .with(deliveredRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
