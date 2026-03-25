package com.example.mscuentas.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.clientes}")
    private String exchangeName;

    @Value("${rabbitmq.queue.clientes}")
    private String queueName;

    @Value("${rabbitmq.routing-key.pattern}")
    private String routingKeyPattern;

    @Bean
    public Queue clientesQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public TopicExchange clientesExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Binding clientesBinding(Queue clientesQueue, TopicExchange clientesExchange) {
        return BindingBuilder.bind(clientesQueue).to(clientesExchange).with(routingKeyPattern);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
