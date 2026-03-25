package com.example.mscuentas.controller;

import com.example.mscuentas.dto.CuentaRequestDTO;
import com.example.mscuentas.dto.CuentaResponseDTO;
import com.example.mscuentas.service.CuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping
    public ResponseEntity<List<CuentaResponseDTO>> findAll() {
        return ResponseEntity.ok(cuentaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CuentaResponseDTO> create(@Valid @RequestBody CuentaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CuentaResponseDTO> update(@PathVariable Long id,
                                                     @Valid @RequestBody CuentaRequestDTO dto) {
        return ResponseEntity.ok(cuentaService.update(id, dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CuentaResponseDTO> partialUpdate(@PathVariable Long id,
                                                            @RequestBody CuentaRequestDTO dto) {
        return ResponseEntity.ok(cuentaService.partialUpdate(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cuentaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
