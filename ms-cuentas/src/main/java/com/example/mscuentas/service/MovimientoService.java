package com.example.mscuentas.service;

import com.example.mscuentas.dto.MovimientoRequestDTO;
import com.example.mscuentas.dto.MovimientoResponseDTO;

import java.util.List;

public interface MovimientoService {

    List<MovimientoResponseDTO> findAll();

    MovimientoResponseDTO findById(Long id);

    MovimientoResponseDTO registrar(MovimientoRequestDTO dto);

    MovimientoResponseDTO update(Long id, MovimientoRequestDTO dto);

    void delete(Long id);
}
