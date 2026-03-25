package com.example.mscuentas.service;

import com.example.mscuentas.domain.entity.Cuenta;
import com.example.mscuentas.domain.entity.TipoCuenta;
import com.example.mscuentas.domain.repository.CuentaRepository;
import com.example.mscuentas.domain.repository.MovimientoRepository;
import com.example.mscuentas.dto.MovimientoRequestDTO;
import com.example.mscuentas.exception.ResourceNotFoundException;
import com.example.mscuentas.exception.SaldoInsuficienteException;
import com.example.mscuentas.service.impl.MovimientoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceImplTest {

    @Mock
    private MovimientoRepository movimientoRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @InjectMocks
    private MovimientoServiceImpl movimientoService;

    @Test
    void registrar_debe_lanzar_SaldoInsuficienteException_cuando_saldo_insuficiente() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(1L);
        cuenta.setNumeroCuenta("496825");
        cuenta.setTipoCuenta(TipoCuenta.AHORRO);
        cuenta.setSaldoInicial(new BigDecimal("540.00"));
        cuenta.setSaldoDisponible(new BigDecimal("540.00"));
        cuenta.setEstado(true);
        cuenta.setClienteId(2L);

        when(cuentaRepository.findByIdWithLock(1L)).thenReturn(Optional.of(cuenta));

        MovimientoRequestDTO dto = new MovimientoRequestDTO();
        dto.setCuentaId(1L);
        dto.setValor(new BigDecimal("-541.00")); // más que el saldo disponible

        assertThatThrownBy(() -> movimientoService.registrar(dto))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessage("Saldo no disponible");
    }

    @Test
    void registrar_debe_lanzar_ResourceNotFoundException_cuando_cuenta_no_existe() {
        when(cuentaRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        MovimientoRequestDTO dto = new MovimientoRequestDTO();
        dto.setCuentaId(999L);
        dto.setValor(new BigDecimal("-100.00"));

        assertThatThrownBy(() -> movimientoService.registrar(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
