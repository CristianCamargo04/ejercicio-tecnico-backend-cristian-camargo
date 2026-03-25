package com.example.mscuentas.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class MovimientoRequestDTO {

    @NotNull(message = "El cuentaId es obligatorio")
    private Long cuentaId;

    @NotNull(message = "El valor es obligatorio")
    private BigDecimal valor;
}
