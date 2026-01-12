package com.bordero.bordero.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * DTO para request de geração de borderô
 * Validações usando Bean Validation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GerarBorderoRequest {

    /**
     * CNPJ do cliente que está solicitando a geração do borderô
     * Pode ser null se não for obrigatório
     */
    private String cnpjCliente;

    /**
     * Lista de IDs das NFes que farão parte do borderô
     * Obrigatório e não pode ser vazio
     */
    @NotNull(message = "Lista de NFe IDs não pode ser nula")
    @NotEmpty(message = "Lista de NFe IDs não pode ser vazia")
    private List<Long> nfeIds;
}