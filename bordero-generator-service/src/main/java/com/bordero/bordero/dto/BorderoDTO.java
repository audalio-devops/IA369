package com.bordero.bordero.dto;

import com.bordero.bordero.domain.model.StatusBordero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para Borderô - evita loop infinito de serialização JSON
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorderoDTO {

    private Long id;
    private String numeroBordero;
    private LocalDateTime dataGeracao;

    // Dados do Cedente
    private String cnpjCedente;
    private String nomeCedente;

    // Dados do Cliente
    private String cnpjCliente;

    // Dados do Fundo
    private String cnpjFundo;
    private String nomeFundo;

    // Títulos (sem referência circular)
    @Builder.Default
    private List<TituloBorderoDTO> titulos = new ArrayList<>();

    // Valores financeiros
    private BigDecimal valorBruto;
    private BigDecimal taxaDesagio;
    private BigDecimal valorDesagio;
    private BigDecimal valorTarifas;
    private BigDecimal valorLiquido;
    private String tarifasDetalhamento;

    // Estatísticas
    private Integer quantidadeTitulos;
    private Integer quantidadeSacados;
    private Integer prazoMedio;
    private Integer prazoMedioDiasUteis;
    private LocalDateTime vencimentoMenor;
    private LocalDateTime vencimentoMaior;

    // Status e datas
    private StatusBordero status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}