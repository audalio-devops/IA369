package com.bordero.bordero.client.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDTO {
    private Long id;
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String email;
    private String status;
    private BigDecimal limiteCredito;
    private BigDecimal limiteDisponivel;
    private LocalDateTime dataCadastro;
}