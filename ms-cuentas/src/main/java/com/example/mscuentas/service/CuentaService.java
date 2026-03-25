package com.example.mscuentas.service;

import com.example.mscuentas.dto.CuentaRequestDTO;
import com.example.mscuentas.dto.CuentaResponseDTO;

import java.util.List;

public interface CuentaService {

    List<CuentaResponseDTO> findAll();

    CuentaResponseDTO findById(Long id);

    CuentaResponseDTO create(CuentaRequestDTO dto);

    CuentaResponseDTO update(Long id, CuentaRequestDTO dto);

    CuentaResponseDTO partialUpdate(Long id, CuentaRequestDTO dto);

    void delete(Long id);
}
