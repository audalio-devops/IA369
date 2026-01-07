package com.bordero.nfe.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response para processamento em lote de NFes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessamentoBatchResponse {

    private int totalArquivos;

    @Builder.Default
    private int sucessos = 0;

    @Builder.Default
    private int erros = 0;

    private String resumo;

    @Builder.Default
    private LocalDateTime dataProcessamento = LocalDateTime.now();

    @Builder.Default
    private List<ResultadoProcessamento> resultados = new ArrayList<>();

    /**
     * Incrementa contador de sucessos
     */
    public void incrementarSucessos() {
        this.sucessos++;
    }

    /**
     * Incrementa contador de erros
     */
    public void incrementarErros() {
        this.erros++;
    }

    /**
     * Adiciona resultado individual
     */
    public void addResultado(ResultadoProcessamento resultado) {
        this.resultados.add(resultado);
    }

    /**
     * Gera resumo textual do processamento
     */
    public void gerarResumo() {
        double taxaSucesso = totalArquivos > 0
                ? (sucessos * 100.0 / totalArquivos)
                : 0;

        this.resumo = String.format(
                "Processados %d arquivos: %d com sucesso (%.1f%%), %d com erro",
                totalArquivos, sucessos, taxaSucesso, erros
        );
    }
}

