package com.bordero.bordero.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "borderos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bordero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroBordero;

    @Column(nullable = false)
    private LocalDateTime dataGeracao;

    @Column(nullable = false, length = 14)
    private String cnpjCedente;

    @Column(nullable = false)
    private String nomeCedente;

    @Column(length = 14)
    private String cnpjFundo;

    @Column
    private String nomeFundo;

    @OneToMany(mappedBy = "bordero", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TituloBordero> titulos = new ArrayList<>();

    // Valores
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorBruto;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxaDesagio = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valorDesagio = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valorTarifas = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorLiquido;

    @Column(columnDefinition = "TEXT")
    private String tarifasDetalhamento; // JSON com detalhamento das tarifas    
    
    // Estatísticas
    @Column
    private Integer quantidadeTitulos;

    @Column
    private Integer prazoMedio;

    @Column
    private LocalDateTime vencimentoMenor;

    @Column
    private LocalDateTime vencimentoMaior;

    @Column
    private Integer prazoMedioDiasUteis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusBordero status = StatusBordero.RASCUNHO;

    @Column(updatable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
        if (numeroBordero == null) {
            numeroBordero = gerarNumeroBordero();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    public void addTitulo(TituloBordero titulo) {
        titulos.add(titulo);
        titulo.setBordero(this);
    }

    private String gerarNumeroBordero() {
        return "BOR" + System.currentTimeMillis();
    }

}

