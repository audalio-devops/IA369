package com.bordero.nfe.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "duplicatas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Duplicata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_fiscal_id", nullable = false)
    private NotaFiscal notaFiscal;

    @Column(nullable = false, length = 60)
    private String numeroDuplicata;

    @Column(nullable = false)
    private LocalDateTime dataVencimento;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusDuplicata status = StatusDuplicata.PENDENTE;

    @Column(name = "bordero_id")
    private Long borderoId;
}
