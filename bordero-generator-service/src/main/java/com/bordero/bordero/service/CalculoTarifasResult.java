package com.bordero.bordero.service;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map; /**
 * Classe para retornar resultado do cálculo de tarifas
 */
@Getter
@Setter
public class CalculoTarifasResult {
    private BigDecimal tarifasPorDocumento = BigDecimal.ZERO;
    private BigDecimal tarifasPorCliente = BigDecimal.ZERO;
    private BigDecimal tarifasGerais = BigDecimal.ZERO;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private Map<String, String> detalhamento = new HashMap<>();

    public void adicionarDetalhamento(String chave, String valor) {
        detalhamento.put(chave, valor);
    }
}
