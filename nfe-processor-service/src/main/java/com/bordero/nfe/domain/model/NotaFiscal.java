package com.bordero.nfe.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notas_fiscais")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotaFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 44)
    private String chaveAcesso;

    @Column(nullable = false)
    private String numeroNfe;

    @Column(nullable = false)
    private String serie;

    @Column(nullable = false)
    private LocalDateTime dataEmissao;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTributos;

    // Emitente (Cedente)
    @Column(nullable = false, length = 14)
    private String cnpjEmitente;

    @Column(nullable = false)
    private String nomeEmitente;

    @Column(length = 100)
    private String nomeFantasiaEmitente;

    // Destinatário (Sacado)
    @Column(nullable = false, length = 14)
    private String cnpjDestinatario;

    @Column(nullable = false)
    private String nomeDestinatario;

    @Column(length = 14)
    private String inscricaoEstadualDestinatario;

    @Column(length = 100)
    private String emailDestinatario;

    // Protocolo
    @Column(length = 20)
    private String numeroProtocolo;

    @Column(length = 10)
    private String statusAutorizacao;

    private LocalDateTime dataAutorizacao;

    // Produtos
    @OneToMany(mappedBy = "notaFiscal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemNotaFiscal> itens = new ArrayList<>();

    // Duplicatas
    @OneToMany(mappedBy = "notaFiscal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Duplicata> duplicatas = new ArrayList<>();

    // Status do processamento
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusProcessamento status = StatusProcessamento.PENDENTE;

    @Column(columnDefinition = "TEXT")
    private String xmlOriginal;

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

    public void addItem(ItemNotaFiscal item) {
        itens.add(item);
        item.setNotaFiscal(this);
    }

    public void addDuplicata(Duplicata duplicata) {
        duplicatas.add(duplicata);
        duplicata.setNotaFiscal(this);
    }
}

