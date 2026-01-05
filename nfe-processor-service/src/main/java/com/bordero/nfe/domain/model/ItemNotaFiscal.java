package com.bordero.nfe.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_nota_fiscal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemNotaFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_fiscal_id", nullable = false)
    private NotaFiscal notaFiscal;

    @Column(nullable = false)
    private Integer numeroItem;

    @Column(nullable = false, length = 60)
    private String codigoProduto;

    @Column(nullable = false)
    private String descricao;

    @Column(length = 8)
    private String ncm;

    @Column(length = 4)
    private String cfop;

    @Column(nullable = false)
    private String unidadeComercial;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantidadeComercial;

    @Column(nullable = false, precision = 15, scale = 10)
    private BigDecimal valorUnitarioComercial;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;
}
