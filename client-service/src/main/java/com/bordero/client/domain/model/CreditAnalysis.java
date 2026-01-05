package com.bordero.client.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_analysis")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // Score de Crédito
    private Integer scoreProprio;
    private Integer scoreSerasa;

    // Análise Financeira
    @Column(precision = 15, scale = 2)
    private BigDecimal faturamentoMensal;

    @Column(precision = 15, scale = 2)
    private BigDecimal patrimonioLiquido;

    @Column(precision = 5, scale = 2)
    private BigDecimal margemLucro;

    // Histórico
    private Integer quantidadeProtestos;
    private Boolean temRestricaoCredito;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    // Decisão
    @Enumerated(EnumType.STRING)
    private AnalysisDecision decisao;

    @Column(precision = 15, scale = 2)
    private BigDecimal limiteAprovado;

    @Column(precision = 5, scale = 2)
    private BigDecimal taxaDesagioSugerida;

    private LocalDateTime dataAnalise;
    private String analistaNome;
    private LocalDateTime dataValidade; // Validade da análise (ex: 6 meses)
}

