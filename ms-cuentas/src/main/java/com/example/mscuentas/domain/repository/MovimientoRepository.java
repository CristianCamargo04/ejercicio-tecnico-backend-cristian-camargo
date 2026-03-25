package com.example.mscuentas.domain.repository;

import com.example.mscuentas.domain.entity.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    @Query("SELECT m FROM Movimiento m JOIN m.cuenta c " +
           "WHERE c.clienteId = :clienteId " +
           "AND m.fecha BETWEEN :inicio AND :fin " +
           "ORDER BY m.fecha ASC")
    List<Movimiento> findByClienteIdAndFechaBetween(
            @Param("clienteId") Long clienteId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);
}
