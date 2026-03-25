package com.example.mscuentas.service.impl;

import com.example.mscuentas.domain.entity.ClienteRef;
import com.example.mscuentas.domain.entity.Movimiento;
import com.example.mscuentas.domain.repository.ClienteRefRepository;
import com.example.mscuentas.domain.repository.MovimientoRepository;
import com.example.mscuentas.dto.ReporteDTO;
import com.example.mscuentas.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private final MovimientoRepository movimientoRepository;
    private final ClienteRefRepository clienteRefRepository;

    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("d/M/yyyy");

    @Override
    public List<ReporteDTO> getEstadoCuenta(LocalDate fechaInicio, LocalDate fechaFin, Long clienteId) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        String nombreCliente = clienteRefRepository.findById(clienteId)
                .map(ClienteRef::getNombre)
                .orElse("Desconocido");

        List<Movimiento> movimientos = movimientoRepository
                .findByClienteIdAndFechaBetween(clienteId, inicio, fin);

        return movimientos.stream()
                .map(m -> new ReporteDTO(
                        m.getFecha().format(FECHA_FORMATTER),
                        nombreCliente,
                        m.getCuenta().getNumeroCuenta(),
                        m.getCuenta().getTipoCuenta().name().charAt(0)
                                + m.getCuenta().getTipoCuenta().name().substring(1).toLowerCase(),
                        m.getCuenta().getSaldoInicial(),
                        m.getCuenta().getEstado(),
                        m.getValor(),
                        m.getSaldo()))
                .collect(Collectors.toList());
    }
}
