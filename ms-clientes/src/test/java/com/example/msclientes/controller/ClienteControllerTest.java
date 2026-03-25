package com.example.msclientes.controller;

import com.example.msclientes.dto.ClienteResponseDTO;
import com.example.msclientes.exception.GlobalExceptionHandler;
import com.example.msclientes.exception.ResourceNotFoundException;
import com.example.msclientes.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
@Import(GlobalExceptionHandler.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClienteService clienteService;

    @Test
    void getById_debe_retornar_200_con_cliente_existente() throws Exception {
        ClienteResponseDTO dto = new ClienteResponseDTO(
                1L, "Jose Lema", "M", 30,
                "1234567890", "Otavalo sn y principal", "098254785", true);

        when(clienteService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/clientes/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(1))
                .andExpect(jsonPath("$.nombre").value("Jose Lema"))
                .andExpect(jsonPath("$.estado").value(true));
    }

    @Test
    void getById_debe_retornar_404_cuando_no_existe() throws Exception {
        when(clienteService.findById(9999L))
                .thenThrow(new ResourceNotFoundException("Cliente no encontrado con id: 9999"));

        mockMvc.perform(get("/api/clientes/9999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cliente no encontrado con id: 9999"));
    }
}
