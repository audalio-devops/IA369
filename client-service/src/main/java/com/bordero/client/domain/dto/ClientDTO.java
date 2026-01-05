package com.bordero.client.domain.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class ClientDTO {
    private Long id;
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String email;
    private String telefone;
    private String status;
    private BigDecimal limiteCredito;
    private BigDecimal limiteDisponivel;
    private LocalDateTime dataCadastro;
    private AddressDTO endereco;
    private CreditAnalysisDTO analiseCredito;
}