package com.example.msclientes.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cliente")
@PrimaryKeyJoinColumn(name = "cliente_id")
public class Cliente extends Persona {

    @NotBlank
    @Column(nullable = false, length = 100)
    private String contrasena;

    @NotNull
    @Column(nullable = false)
    private Boolean estado = true;
}
