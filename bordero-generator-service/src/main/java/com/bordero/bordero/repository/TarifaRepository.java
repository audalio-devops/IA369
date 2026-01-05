package com.bordero.bordero.repository;

import com.bordero.bordero.domain.model.Tarifa;
import com.bordero.bordero.domain.model.TipoTarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    List<Tarifa> findByAtivaTrue();
    List<Tarifa> findByTipoAndAtivaTrue(TipoTarifa tipo);
    Optional<Tarifa> findByCodigo(String codigo);
}