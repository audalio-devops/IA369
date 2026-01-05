package com.bordero.nfe.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadNFeRequest {
    @NotBlank(message = "O XML não pode estar vazio")
    private String xmlContent;

    @NotBlank(message = "O CNPJ do cliente é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos")
    private String cnpjCliente;
}
