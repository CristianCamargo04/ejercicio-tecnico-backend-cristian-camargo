package com.example.mscuentas.integration;

import com.example.mscuentas.domain.entity.*;
import com.example.mscuentas.domain.repository.ClienteRefRepository;
import com.example.mscuentas.domain.repository.CuentaRepository;
import com.example.mscuentas.domain.repository.MovimientoRepository;
import com.example.mscuentas.dto.ReporteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReporteControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClienteRefRepository clienteRefRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @BeforeEach
    void setUp() {
        movimientoRepository.deleteAll();
        cuentaRepository.deleteAll();
        clienteRefRepository.deleteAll();

        // Datos del PDF: Marianela Montalvo, clienteId=2
        ClienteRef cliente = new ClienteRef(2L, "Marianela Montalvo");
        clienteRefRepository.save(cliente);

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("225487");
        cuenta.setTipoCuenta(TipoCuenta.CORRIENTE);
        cuenta.setSaldoInicial(new BigDecimal("100.00"));
        cuenta.setSaldoDisponible(new BigDecimal("700.00"));
        cuenta.setEstado(true);
        cuenta.setClienteId(2L);
        cuentaRepository.save(cuenta);

        Movimiento movimiento = new Movimiento();
        movimiento.setFecha(LocalDateTime.of(2022, 2, 10, 10, 0));
        movimiento.setTipoMovimiento(TipoMovimiento.DEPOSITO);
        movimiento.setValor(new BigDecimal("600.00"));
        movimiento.setSaldo(new BigDecimal("700.00"));
        movimiento.setCuenta(cuenta);
        movimientoRepository.save(movimiento);
    }

    @Test
    void getReporte_debe_retornar_movimientos_del_cliente_en_rango_de_fechas() {
        String url = "http://localhost:" + port +
                "/api/reportes?fechaInicio=2022-02-01&fechaFin=2022-02-28&clienteId=2";

        ReporteDTO[] result = restTemplate.getForObject(url, ReporteDTO[].class);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result[0].getCliente()).isEqualTo("Marianela Montalvo");
        assertThat(result[0].getNumeroCuenta()).isEqualTo("225487");
        assertThat(result[0].getSaldoDisponible()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(result[0].getMovimiento()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    void getReporte_debe_retornar_lista_vacia_cuando_no_hay_movimientos_en_rango() {
        String url = "http://localhost:" + port +
                "/api/reportes?fechaInicio=2023-01-01&fechaFin=2023-01-31&clienteId=2";

        ReporteDTO[] result = restTemplate.getForObject(url, ReporteDTO[].class);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
}
