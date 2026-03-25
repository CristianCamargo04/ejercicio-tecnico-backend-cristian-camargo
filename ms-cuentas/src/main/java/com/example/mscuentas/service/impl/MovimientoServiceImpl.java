package com.example.mscuentas.service.impl;

import com.example.mscuentas.domain.entity.Cuenta;
import com.example.mscuentas.domain.entity.Movimiento;
import com.example.mscuentas.domain.entity.TipoMovimiento;
import com.example.mscuentas.domain.repository.CuentaRepository;
import com.example.mscuentas.domain.repository.MovimientoRepository;
import com.example.mscuentas.dto.MovimientoRequestDTO;
import com.example.mscuentas.dto.MovimientoResponseDTO;
import com.example.mscuentas.exception.ResourceNotFoundException;
import com.example.mscuentas.exception.SaldoInsuficienteException;
import com.example.mscuentas.service.MovimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovimientoServiceImpl implements MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final CuentaRepository cuentaRepository;

    @Override
    public List<MovimientoResponseDTO> findAll() {
        return movimientoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MovimientoResponseDTO findById(Long id) {
        return toResponseDTO(getMovimientoOrThrow(id));
    }

    @Override
    @Transactional
    public MovimientoResponseDTO registrar(MovimientoRequestDTO dto) {
        Cuenta cuenta = cuentaRepository.findByIdWithLock(dto.getCuentaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cuenta no encontrada con id: " + dto.getCuentaId()));

        BigDecimal nuevoSaldo = cuenta.getSaldoDisponible().add(dto.getValor());
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new SaldoInsuficienteException();
        }

        cuenta.setSaldoDisponible(nuevoSaldo);
        cuentaRepository.save(cuenta);

        Movimiento movimiento = new Movimiento();
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setValor(dto.getValor());
        movimiento.setSaldo(nuevoSaldo);
        movimiento.setTipoMovimiento(
                dto.getValor().compareTo(BigDecimal.ZERO) >= 0
                        ? TipoMovimiento.DEPOSITO
                        : TipoMovimiento.RETIRO);
        movimiento.setCuenta(cuenta);

        return toResponseDTO(movimientoRepository.save(movimiento));
    }

    @Override
    @Transactional
    public MovimientoResponseDTO update(Long id, MovimientoRequestDTO dto) {
        Movimiento movimiento = getMovimientoOrThrow(id);
        if (dto.getValor() != null) movimiento.setValor(dto.getValor());
        return toResponseDTO(movimientoRepository.save(movimiento));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        movimientoRepository.delete(getMovimientoOrThrow(id));
    }

    private Movimiento getMovimientoOrThrow(Long id) {
        return movimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Movimiento no encontrado con id: " + id));
    }

    private MovimientoResponseDTO toResponseDTO(Movimiento m) {
        return new MovimientoResponseDTO(
                m.getId(), m.getFecha(), m.getTipoMovimiento(),
                m.getValor(), m.getSaldo(), m.getCuenta().getNumeroCuenta());
    }
}
