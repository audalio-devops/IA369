package com.bordero.client.domain.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditAnalysisDTO {

    private Long id;
    private Long clientId;

    // Score de Crédito
    private Integer scoreProprio;
    private Integer scoreSerasa;

    // Análise Financeira
    private BigDecimal faturamentoMensal;
    private BigDecimal patrimonioLiquido;
    private BigDecimal margemLucro;

    // Histórico
    private Integer quantidadeProtestos;
    private Boolean temRestricaoCredito;
    private String observacoes;

    // Decisão
    private String decisao;
    private BigDecimal limiteAprovado;
    private BigDecimal taxaDesagioSugerida;

    // Metadados
    private LocalDateTime dataAnalise;
    private String analistaNome;
    private LocalDateTime dataValidade;

    // Status calculado
    private Boolean valida;
    private Integer diasParaVencimento;
}