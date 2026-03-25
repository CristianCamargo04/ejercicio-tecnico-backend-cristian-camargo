package com.example.mscuentas.domain.repository;

import com.example.mscuentas.domain.entity.ClienteRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRefRepository extends JpaRepository<ClienteRef, Long> {
}
