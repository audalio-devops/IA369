package com.bordero.bordero.dto;

import lombok.*;

/**
 * DTO para Tipo de Título
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoTituloDTO {

    private Long id;
    private String tipo;
    private String nome;
    private String descricao;
    private Boolean ativo;
}
