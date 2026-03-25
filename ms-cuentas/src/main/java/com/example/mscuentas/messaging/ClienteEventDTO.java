package com.example.mscuentas.messaging;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClienteEventDTO {

    private Long clienteId;
    private String nombre;
    private String identificacion;
    private String accion;
}
