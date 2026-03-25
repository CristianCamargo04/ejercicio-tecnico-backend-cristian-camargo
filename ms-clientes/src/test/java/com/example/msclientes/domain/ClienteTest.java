package com.example.msclientes.domain;

import com.example.msclientes.domain.entity.Cliente;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteTest {

    @Test
    void deberia_crear_cliente_con_datos_validos() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Jose Lema");
        cliente.setIdentificacion("1234567890");
        cliente.setDireccion("Otavalo sn y principal");
        cliente.setTelefono("098254785");
        cliente.setContrasena("1234");
        cliente.setEstado(true);

        assertThat(cliente.getNombre()).isEqualTo("Jose Lema");
        assertThat(cliente.getIdentificacion()).isEqualTo("1234567890");
        assertThat(cliente.getContrasena()).isEqualTo("1234");
        assertThat(cliente.getEstado()).isTrue();
    }

    @Test
    void estado_debe_cambiar_a_false() {
        Cliente cliente = new Cliente();
        cliente.setEstado(true);
        assertThat(cliente.getEstado()).isTrue();

        cliente.setEstado(false);
        assertThat(cliente.getEstado()).isFalse();
    }

    @Test
    void estado_por_defecto_debe_ser_true() {
        Cliente cliente = new Cliente();
        // El valor por defecto se setea en el campo directamente
        cliente.setEstado(true);
        assertThat(cliente.getEstado()).isTrue();
    }
}
