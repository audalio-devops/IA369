package com.bordero.nfe.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DestinatarioDTO {
    private String cnpj;
    private String razaoSocial;
    private String inscricaoEstadual;
    private String email;
}
