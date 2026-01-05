package com.bordero.nfe.domain.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProcessamentoResponse {
    private boolean sucesso;
    private String mensagem;
    private NotaFiscalDTO notaFiscal;
    private List<String> erros;
}