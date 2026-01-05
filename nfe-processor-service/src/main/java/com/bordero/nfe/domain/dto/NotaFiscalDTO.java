package com.bordero.nfe.domain.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class NotaFiscalDTO {
    private Long id;
    private String chaveAcesso;
    private String numeroNfe;
    private String serie;
    private LocalDateTime dataEmissao;
    private BigDecimal valorTotal;
    private BigDecimal valorTributos;
    private EmitenteDTO emitente;
    private DestinatarioDTO destinatario;
    private List<ItemDTO> itens;
    private List<DuplicataDTO> duplicatas;
    private String status;
}

@Data @Builder
class ItemDTO {
    private Integer numeroItem;
    private String codigoProduto;
    private String descricao;
    private String ncm;
    private BigDecimal quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal valorTotal;
}

