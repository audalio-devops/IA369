package com.bordero.bordero.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "titulos_bordero")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TituloBordero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bordero_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Bordero bordero;

    // NOVO CAMPO: Tipo do título (NF, Cheque, etc)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_titulo_id")
    private TipoTitulo tipoTitulo;

    @Column(nullable = false)
    private Long nfeId;

    @Column(nullable = false)
    private Long duplicataId;

    @Column(nullable = false, length = 44)
    private String chaveAcessoNFe;

    @Column(nullable = false)
    private String numeroNFe;

    @Column(nullable = false)
    private String numeroDuplicata;

    @Column(nullable = false)
    private LocalDateTime dataVencimento;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorBruto;

    @Column(nullable = false)
    private Integer diasParaVencimento;

    @Column(nullable = false)
    private Integer diasUteis;

    // NOVO CAMPO: PZ - Prazo adicional (por enquanto sempre 0)
    @Column
    @Builder.Default
    private Integer prazoAdicional = 0;

    // NOVO CAMPO: D+ (Float) - dias de compensação bancária
    @Column
    private Integer floatDias;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxaDesagio;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorDesagio;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorLiquido;

    @Column
    private LocalDate dataCompensacao;

    @Column(precision = 10, scale = 2)
    private BigDecimal tarifaDocumento;

    // Sacado
    @Column(nullable = false, length = 14)
    private String cnpjSacado;

    @Column(nullable = false)
    private String nomeSacado;

    // Emitente (para facilitar exibição no PDF)
    @Column(length = 14)
    private String cnpjEmitente;

    @Column
    private String nomeEmitente;
}