package com.bordero.nfe.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List; /**
 * Resultado do processamento de um arquivo individual
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoProcessamento {

    private String nomeArquivo;

    private boolean sucesso;

    private String mensagem;

    private String chaveAcesso;

    private Long nfeId;

    private String numeroNota;

    private BigDecimal valorTotal;

    private Integer quantidadeDuplicatas;

    @Builder.Default
    private LocalDateTime dataProcessamento = LocalDateTime.now();

    private List<String> erros;
}
