package com.bordero.client.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType tipo;

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(nullable = false)
    private String caminhoArquivo;

    private Long tamanhoBytes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDENTE;

    private LocalDateTime dataUpload;
    private LocalDateTime dataValidacao;
    private String validadoPor;
    private String observacoes;

    @PrePersist
    protected void onCreate() {
        dataUpload = LocalDateTime.now();
    }
}

