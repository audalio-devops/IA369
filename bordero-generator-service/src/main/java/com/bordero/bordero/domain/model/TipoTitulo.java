package com.bordero.bordero.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tipos_titulo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TipoTitulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String tipo; // Ex: "NF"

    @Column(nullable = false, length = 100)
    private String nome; // Ex: "Nota Fiscal"

    @Column(columnDefinition = "TEXT")
    private String descricao; // Ex: "Título existente em uma nota fiscal"

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    // Auditoria
    @Column(updatable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}