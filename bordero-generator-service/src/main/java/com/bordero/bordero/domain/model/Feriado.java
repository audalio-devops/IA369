package com.bordero.bordero.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "feriados",
        uniqueConstraints = @UniqueConstraint(columnNames = {"data", "tipo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Feriado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoFeriado tipo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    // Para feriados municipais/estaduais
    @Column(length = 2)
    private String uf;

    @Column(length = 7)
    private String codigoMunicipio;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}

