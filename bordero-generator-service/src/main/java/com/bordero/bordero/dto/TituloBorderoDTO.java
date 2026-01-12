package com.bordero.bordero.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para Título do Borderô - sem referência ao Borderô para evitar loop
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TituloBorderoDTO {

    private Long id;

    // ID do borderô (apenas referência, não objeto completo)
    private Long borderoId;
    private String numeroBordero;

    // Tipo de título
    private TipoTituloDTO tipoTitulo;

    // Identificação da NFe e duplicata
    private Long nfeId;
    private Long duplicataId;
    private String chaveAcessoNFe;
    private String numeroNFe;
    private String numeroDuplicata;

    // Datas e prazos
    private LocalDateTime dataVencimento;
    private LocalDate dataCompensacao;
    private Integer diasParaVencimento;
    private Integer diasUteis;
    private Integer prazoAdicional;
    private Integer floatDias; // D+

    // Valores financeiros
    private BigDecimal valorBruto;
    private BigDecimal taxaDesagio;
    private BigDecimal valorDesagio;
    private BigDecimal valorLiquido;
    private BigDecimal tarifaDocumento;

    // Dados do Sacado
    private String cnpjSacado;
    private String nomeSacado;

    // Dados do Emitente
    private String cnpjEmitente;
    private String nomeEmitente;
}