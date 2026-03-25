package com.example.mscuentas.service.impl;

import com.example.mscuentas.domain.entity.Cuenta;
import com.example.mscuentas.domain.repository.ClienteRefRepository;
import com.example.mscuentas.domain.repository.CuentaRepository;
import com.example.mscuentas.dto.CuentaRequestDTO;
import com.example.mscuentas.dto.CuentaResponseDTO;
import com.example.mscuentas.exception.BadRequestException;
import com.example.mscuentas.exception.ResourceNotFoundException;
import com.example.mscuentas.service.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRefRepository clienteRefRepository;

    @Override
    public List<CuentaResponseDTO> findAll() {
        return cuentaRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CuentaResponseDTO findById(Long id) {
        return toResponseDTO(getCuentaOrThrow(id));
    }

    @Override
    @Transactional
    public CuentaResponseDTO create(CuentaRequestDTO dto) {
        if (!clienteRefRepository.existsById(dto.getClienteId())) {
            throw new BadRequestException("Cliente no encontrado con id: " + dto.getClienteId());
        }
        cuentaRepository.findByNumeroCuenta(dto.getNumeroCuenta())
                .ifPresent(c -> {
                    throw new BadRequestException(
                            "Ya existe una cuenta con número: " + dto.getNumeroCuenta());
                });

        Cuenta cuenta = new Cuenta();
        applyFields(cuenta, dto);
        cuenta.setSaldoDisponible(dto.getSaldoInicial());
        return toResponseDTO(cuentaRepository.save(cuenta));
    }

    @Override
    @Transactional
    public CuentaResponseDTO update(Long id, CuentaRequestDTO dto) {
        Cuenta cuenta = getCuentaOrThrow(id);
        applyFields(cuenta, dto);
        return toResponseDTO(cuentaRepository.save(cuenta));
    }

    @Override
    @Transactional
    public CuentaResponseDTO partialUpdate(Long id, CuentaRequestDTO dto) {
        Cuenta cuenta = getCuentaOrThrow(id);
        if (dto.getNumeroCuenta() != null) cuenta.setNumeroCuenta(dto.getNumeroCuenta());
        if (dto.getTipoCuenta() != null) cuenta.setTipoCuenta(dto.getTipoCuenta());
        if (dto.getSaldoInicial() != null) cuenta.setSaldoInicial(dto.getSaldoInicial());
        if (dto.getEstado() != null) cuenta.setEstado(dto.getEstado());
        if (dto.getClienteId() != null) cuenta.setClienteId(dto.getClienteId());
        return toResponseDTO(cuentaRepository.save(cuenta));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        cuentaRepository.delete(getCuentaOrThrow(id));
    }

    private Cuenta getCuentaOrThrow(Long id) {
        return cuentaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con id: " + id));
    }

    private void applyFields(Cuenta cuenta, CuentaRequestDTO dto) {
        cuenta.setNumeroCuenta(dto.getNumeroCuenta());
        cuenta.setTipoCuenta(dto.getTipoCuenta());
        cuenta.setSaldoInicial(dto.getSaldoInicial());
        cuenta.setEstado(dto.getEstado() != null ? dto.getEstado() : true);
        cuenta.setClienteId(dto.getClienteId());
    }

    private CuentaResponseDTO toResponseDTO(Cuenta c) {
        return new CuentaResponseDTO(
                c.getId(), c.getNumeroCuenta(), c.getTipoCuenta(),
                c.getSaldoInicial(), c.getSaldoDisponible(), c.getEstado(), c.getClienteId());
    }
}
