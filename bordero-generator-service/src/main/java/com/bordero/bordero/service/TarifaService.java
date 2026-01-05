package com.bordero.bordero.service;

import com.bordero.bordero.domain.model.*;
import com.bordero.bordero.repository.TarifaRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TarifaService {

    private final TarifaRepository tarifaRepository;

    /**
     * Calcula todas as tarifas de um borderô
     */
    public CalculoTarifasResult calcularTarifas(int quantidadeTitulos, boolean incluirConsultaSerasa) {
        CalculoTarifasResult result = new CalculoTarifasResult();

        // 1. Tarifas por documento
        List<Tarifa> tarifasDocumento = tarifaRepository.findByTipoAndAtivaTrue(TipoTarifa.DOCUMENTO);
        BigDecimal totalPorDocumento = tarifasDocumento.stream()
                .map(Tarifa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(new BigDecimal(quantidadeTitulos));

        result.setTarifasPorDocumento(totalPorDocumento);
        result.adicionarDetalhamento("Tarifas por Documento",
                String.format("%d títulos x R$ %.2f", quantidadeTitulos,
                        totalPorDocumento.divide(new BigDecimal(quantidadeTitulos), 2, java.math.RoundingMode.HALF_UP)));

        // 2. Tarifas por cliente
        BigDecimal totalPorCliente = BigDecimal.ZERO;
        List<Tarifa> tarifasCliente = tarifaRepository.findByTipoAndAtivaTrue(TipoTarifa.CLIENTE);

        for (Tarifa tarifa : tarifasCliente) {
            // Consulta Serasa só é cobrada se solicitada
            if (tarifa.getCodigo().equals("SERASA") && !incluirConsultaSerasa) {
                continue;
            }
            totalPorCliente = totalPorCliente.add(tarifa.getValor());
            result.adicionarDetalhamento(tarifa.getNome(),
                    String.format("R$ %.2f", tarifa.getValor()));
        }
        result.setTarifasPorCliente(totalPorCliente);

        // 3. Tarifas gerais
        List<Tarifa> tarifasGerais = tarifaRepository.findByTipoAndAtivaTrue(TipoTarifa.GERAL);
        BigDecimal totalGeral = BigDecimal.ZERO;

        for (Tarifa tarifa : tarifasGerais) {
            totalGeral = totalGeral.add(tarifa.getValor());
            result.adicionarDetalhamento(tarifa.getNome(),
                    String.format("R$ %.2f", tarifa.getValor()));
        }
        result.setTarifasGerais(totalGeral);

        // Total
        result.setValorTotal(
                totalPorDocumento.add(totalPorCliente).add(totalGeral)
        );

        log.info("Tarifas calculadas - Doc: {}, Cliente: {}, Geral: {}, Total: {}",
                totalPorDocumento, totalPorCliente, totalGeral, result.getValorTotal());

        return result;
    }

    /**
     * Busca tarifa específica por código
     */
    public Tarifa buscarPorCodigo(String codigo) {
        return tarifaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("Tarifa não encontrada: " + codigo));
    }

    /**
     * Lista todas as tarifas ativas
     */
    public List<Tarifa> listarAtivas() {
        return tarifaRepository.findByAtivaTrue();
    }
}

