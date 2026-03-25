package com.example.msclientes.service;

import com.example.msclientes.dto.ClienteRequestDTO;
import com.example.msclientes.dto.ClienteResponseDTO;

import java.util.List;

public interface ClienteService {

    List<ClienteResponseDTO> findAll();

    ClienteResponseDTO findById(Long id);

    ClienteResponseDTO create(ClienteRequestDTO dto);

    ClienteResponseDTO update(Long id, ClienteRequestDTO dto);

    ClienteResponseDTO partialUpdate(Long id, ClienteRequestDTO dto);

    void delete(Long id);
}
