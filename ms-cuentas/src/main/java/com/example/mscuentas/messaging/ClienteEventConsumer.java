package com.example.mscuentas.messaging;

import com.example.mscuentas.domain.entity.ClienteRef;
import com.example.mscuentas.domain.repository.ClienteRefRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class ClienteEventConsumer {

    private final ClienteRefRepository clienteRefRepository;

    @RabbitListener(queues = "${rabbitmq.queue.clientes}")
    public void handleClienteEvent(ClienteEventDTO event) {
        try {
            if (event == null || event.getClienteId() == null || event.getAccion() == null) {
                log.warn("Mensaje de cliente recibido con datos inválidos, descartando.");
                return;
            }

            switch (event.getAccion().toLowerCase()) {
                case "creado", "actualizado" -> {
                    ClienteRef ref = new ClienteRef(event.getClienteId(), event.getNombre());
                    clienteRefRepository.save(ref);
                    log.info("ClienteRef actualizado: id={}, nombre={}", event.getClienteId(), event.getNombre());
                }
                case "eliminado" -> {
                    clienteRefRepository.deleteById(event.getClienteId());
                    log.info("ClienteRef eliminado: id={}", event.getClienteId());
                }
                default -> log.warn("Acción desconocida en evento de cliente: {}", event.getAccion());
            }
        } catch (Exception e) {
            log.error("Error procesando evento de cliente id={}: {}",
                    event != null ? event.getClienteId() : "null", e.getMessage());
        }
    }
}
