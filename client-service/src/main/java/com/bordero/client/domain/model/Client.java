package com.bordero.client.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 14)
    private String cnpj;

    @Column(nullable = false)
    private String razaoSocial;

    private String nomeFantasia;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 15)
    private String telefone;

    private String inscricaoEstadual;
    private String inscricaoMunicipal;

    @Embedded
    private ClientAddress endereco;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ClientDocument> documentos = new ArrayList<>();

    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL)
    private CreditAnalysis analiseCredito;

    // Dados Financeiros
    @Column(precision = 15, scale = 2)
    private BigDecimal limiteCredito;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal limiteDisponivel = BigDecimal.ZERO;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClientStatus status = ClientStatus.PENDENTE;

    private Boolean ativo = true;

    // Auditoria
    @Column(updatable = false)
    private LocalDateTime dataCadastro;

    private LocalDateTime dataAtualizacao;

    private String usuarioCadastro;

    @PrePersist
    protected void onCreate() {
        dataCadastro = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}

