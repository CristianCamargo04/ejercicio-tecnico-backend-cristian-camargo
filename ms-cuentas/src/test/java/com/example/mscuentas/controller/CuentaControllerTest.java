package com.example.mscuentas.controller;

import com.example.mscuentas.dto.CuentaResponseDTO;
import com.example.mscuentas.domain.entity.TipoCuenta;
import com.example.mscuentas.exception.GlobalExceptionHandler;
import com.example.mscuentas.exception.ResourceNotFoundException;
import com.example.mscuentas.service.CuentaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CuentaController.class)
@Import(GlobalExceptionHandler.class)
class CuentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CuentaService cuentaService;

    @Test
    void getById_debe_retornar_200_con_cuenta_existente() throws Exception {
        CuentaResponseDTO dto = new CuentaResponseDTO(
                1L, "478758", TipoCuenta.AHORRO,
                new BigDecimal("2000.00"), new BigDecimal("2000.00"), true, 1L);

        when(cuentaService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/cuentas/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numeroCuenta").value("478758"))
                .andExpect(jsonPath("$.tipoCuenta").value("AHORRO"));
    }

    @Test
    void getById_debe_retornar_404_cuando_no_existe() throws Exception {
        when(cuentaService.findById(9999L))
                .thenThrow(new ResourceNotFoundException("Cuenta no encontrada con id: 9999"));

        mockMvc.perform(get("/api/cuentas/9999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cuenta no encontrada con id: 9999"));
    }
}
