package com.example.msclientes.service.impl;

import com.example.msclientes.domain.entity.Cliente;
import com.example.msclientes.domain.repository.ClienteRepository;
import com.example.msclientes.dto.ClienteRequestDTO;
import com.example.msclientes.dto.ClienteResponseDTO;
import com.example.msclientes.exception.BadRequestException;
import com.example.msclientes.exception.ResourceNotFoundException;
import com.example.msclientes.messaging.ClienteEventDTO;
import com.example.msclientes.messaging.ClienteEventPublisher;
import com.example.msclientes.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteEventPublisher eventPublisher;

    @Override
    public List<ClienteResponseDTO> findAll() {
        return clienteRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ClienteResponseDTO findById(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        return toResponseDTO(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDTO create(ClienteRequestDTO dto) {
        clienteRepository.findByIdentificacion(dto.getIdentificacion())
                .ifPresent(c -> {
                    throw new BadRequestException(
                            "Ya existe un cliente con identificación: " + dto.getIdentificacion());
                });

        Cliente cliente = toEntity(dto);
        Cliente saved = clienteRepository.save(cliente);

        eventPublisher.publicarClienteCreado(
                new ClienteEventDTO(saved.getId(), saved.getNombre(), saved.getIdentificacion(), "creado"));

        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ClienteResponseDTO update(Long id, ClienteRequestDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        applyFields(cliente, dto);
        Cliente saved = clienteRepository.save(cliente);

        eventPublisher.publicarClienteActualizado(
                new ClienteEventDTO(saved.getId(), saved.getNombre(), saved.getIdentificacion(), "actualizado"));

        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ClienteResponseDTO partialUpdate(Long id, ClienteRequestDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        if (dto.getNombre() != null) cliente.setNombre(dto.getNombre());
        if (dto.getGenero() != null) cliente.setGenero(dto.getGenero());
        if (dto.getEdad() != null) cliente.setEdad(dto.getEdad());
        if (dto.getDireccion() != null) cliente.setDireccion(dto.getDireccion());
        if (dto.getTelefono() != null) cliente.setTelefono(dto.getTelefono());
        if (dto.getContrasena() != null) cliente.setContrasena(dto.getContrasena());
        if (dto.getEstado() != null) cliente.setEstado(dto.getEstado());

        Cliente saved = clienteRepository.save(cliente);

        eventPublisher.publicarClienteActualizado(
                new ClienteEventDTO(saved.getId(), saved.getNombre(), saved.getIdentificacion(), "actualizado"));

        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        clienteRepository.delete(cliente);

        eventPublisher.publicarClienteEliminado(
                new ClienteEventDTO(id, cliente.getNombre(), cliente.getIdentificacion(), "eliminado"));
    }

    private Cliente toEntity(ClienteRequestDTO dto) {
        Cliente cliente = new Cliente();
        applyFields(cliente, dto);
        return cliente;
    }

    private void applyFields(Cliente cliente, ClienteRequestDTO dto) {
        cliente.setNombre(dto.getNombre());
        cliente.setGenero(dto.getGenero());
        cliente.setEdad(dto.getEdad());
        cliente.setIdentificacion(dto.getIdentificacion());
        cliente.setDireccion(dto.getDireccion());
        cliente.setTelefono(dto.getTelefono());
        cliente.setContrasena(dto.getContrasena());
        cliente.setEstado(dto.getEstado() != null ? dto.getEstado() : true);
    }

    private ClienteResponseDTO toResponseDTO(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getGenero(),
                cliente.getEdad(),
                cliente.getIdentificacion(),
                cliente.getDireccion(),
                cliente.getTelefono(),
                cliente.getEstado()
        );
    }
}
