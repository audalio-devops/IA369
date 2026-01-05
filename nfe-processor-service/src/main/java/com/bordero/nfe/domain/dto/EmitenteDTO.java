package com.bordero.nfe.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class EmitenteDTO {
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String inscricaoEstadual;
}
