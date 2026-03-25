package com.example.msclientes.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.clientes}")
    private String exchange;

    @Value("${rabbitmq.routing-key.cliente-creado}")
    private String routingKeyCreado;

    @Value("${rabbitmq.routing-key.cliente-actualizado}")
    private String routingKeyActualizado;

    @Value("${rabbitmq.routing-key.cliente-eliminado}")
    private String routingKeyEliminado;

    public void publicarClienteCreado(ClienteEventDTO event) {
        publish(routingKeyCreado, event);
    }

    public void publicarClienteActualizado(ClienteEventDTO event) {
        publish(routingKeyActualizado, event);
    }

    public void publicarClienteEliminado(ClienteEventDTO event) {
        publish(routingKeyEliminado, event);
    }

    private void publish(String routingKey, ClienteEventDTO event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Evento publicado: routingKey={}, clienteId={}", routingKey, event.getClienteId());
        } catch (Exception e) {
            log.warn("No se pudo publicar evento al broker: routingKey={}, clienteId={}, error={}",
                    routingKey, event.getClienteId(), e.getMessage());
        }
    }
}
