package com.example.msclientes.service;

import com.example.msclientes.domain.entity.Cliente;
import com.example.msclientes.domain.repository.ClienteRepository;
import com.example.msclientes.dto.ClienteRequestDTO;
import com.example.msclientes.exception.BadRequestException;
import com.example.msclientes.exception.ResourceNotFoundException;
import com.example.msclientes.messaging.ClienteEventPublisher;
import com.example.msclientes.service.impl.ClienteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteEventPublisher eventPublisher;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    @Test
    void findById_debe_lanzar_excepcion_cuando_no_existe() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void create_debe_lanzar_excepcion_cuando_identificacion_duplicada() {
        Cliente existente = new Cliente();
        existente.setIdentificacion("1234567890");
        when(clienteRepository.findByIdentificacion("1234567890"))
                .thenReturn(Optional.of(existente));

        ClienteRequestDTO dto = new ClienteRequestDTO();
        dto.setNombre("Test");
        dto.setIdentificacion("1234567890");
        dto.setContrasena("pass");
        dto.setEstado(true);

        assertThatThrownBy(() -> clienteService.create(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("1234567890");
    }
}
