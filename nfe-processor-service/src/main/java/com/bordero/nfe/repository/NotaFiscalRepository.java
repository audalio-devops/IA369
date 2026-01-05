package com.bordero.nfe.repository;

import com.bordero.nfe.domain.model.NotaFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Long> {
    boolean existsByChaveAcesso(String chaveAcesso);
    Optional<NotaFiscal> findByChaveAcesso(String chaveAcesso);
}
