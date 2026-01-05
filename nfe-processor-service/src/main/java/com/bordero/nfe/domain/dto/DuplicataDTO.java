package com.bordero.nfe.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class DuplicataDTO {
    private String numero;
    private LocalDateTime vencimento;
    private BigDecimal valor;
    private String status;
}
