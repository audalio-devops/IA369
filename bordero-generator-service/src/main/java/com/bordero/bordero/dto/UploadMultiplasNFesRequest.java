package com.bordero.bordero.dto;

import lombok.*;
import java.util.List;

/**
 * DTO para receber upload de múltiplas NFes para criar um único borderô
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadMultiplasNFesRequest {

    private String cnpjCliente;

    private List<Long> nfeIds; // Lista de IDs das NFes já processadas
}
