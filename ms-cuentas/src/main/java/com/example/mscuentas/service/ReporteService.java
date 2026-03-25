package com.example.mscuentas.service;

import com.example.mscuentas.dto.ReporteDTO;

import java.time.LocalDate;
import java.util.List;

public interface ReporteService {

    List<ReporteDTO> getEstadoCuenta(LocalDate fechaInicio, LocalDate fechaFin, Long clienteId);
}
