package com.bordero.bordero.repository;

import com.bordero.bordero.domain.model.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FeriadoRepository extends JpaRepository<Feriado, Long> {

    @Query("""
        SELECT f FROM Feriado f 
        WHERE f.data = :data 
        AND f.ativo = true
        AND (f.tipo = 'NACIONAL' 
             OR f.tipo = 'BANCARIO'
             OR (f.tipo = 'ESTADUAL' AND f.uf = :uf)
             OR (f.tipo = 'MUNICIPAL' AND f.codigoMunicipio = :codigoMunicipio))
        """)
    List<Feriado> findByDataAndLocalidade(
            @Param("data") LocalDate data,
            @Param("uf") String uf,
            @Param("codigoMunicipio") String codigoMunicipio
    );

    List<Feriado> findByDataBetweenAndAtivoTrue(LocalDate inicio, LocalDate fim);
}