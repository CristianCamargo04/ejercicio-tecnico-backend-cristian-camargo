package com.example.msclientes.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClienteEventDTO {

    private Long clienteId;
    private String nombre;
    private String identificacion;
    private String accion;
}
