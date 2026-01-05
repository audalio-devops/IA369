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
    private Bordero bordero;

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
}
