package com.bordero.bordero.repository;

import com.bordero.bordero.domain.model.TipoTitulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TipoTituloRepository extends JpaRepository<TipoTitulo, Long> {

    Optional<TipoTitulo> findByTipo(String tipo);

    Optional<TipoTitulo> findByTipoAndAtivoTrue(String tipo);
}
