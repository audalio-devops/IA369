package com.bordero.client.domain.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ClientRegistrationRequest {

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos")
    private String cnpj;

    @NotBlank(message = "Razão Social é obrigatória")
    @Size(min = 3, max = 200)
    private String razaoSocial;

    private String nomeFantasia;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 dígitos")
    private String telefone;

    private String inscricaoEstadual;

    @NotNull(message = "Endereço é obrigatório")
    private AddressDTO endereco;
}

