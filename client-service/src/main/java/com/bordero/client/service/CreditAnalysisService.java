package com.bordero.client.service;

import com.bordero.client.domain.dto.CreditAnalysisDTO;
import com.bordero.client.domain.model.*;
import com.bordero.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreditAnalysisService {

    private final ClientRepository clientRepository;

    /**
     * Realiza análise de crédito manual
     */
    @Transactional
    public CreditAnalysisDTO realizarAnaliseManual(Long clientId, CreditAnalysisDTO request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Criar ou atualizar análise
        CreditAnalysis analysis = client.getAnaliseCredito();
        if (analysis == null) {
            analysis = new CreditAnalysis();
            analysis.setClient(client);
        }

        // Preencher dados
        analysis.setScoreProprio(request.getScoreProprio());
        analysis.setScoreSerasa(request.getScoreSerasa());
        analysis.setFaturamentoMensal(request.getFaturamentoMensal());
        analysis.setPatrimonioLiquido(request.getPatrimonioLiquido());
        analysis.setMargemLucro(request.getMargemLucro());
        analysis.setQuantidadeProtestos(request.getQuantidadeProtestos());
        analysis.setTemRestricaoCredito(request.getTemRestricaoCredito());
        analysis.setObservacoes(request.getObservacoes());
        analysis.setAnalistaNome(request.getAnalistaNome());
        analysis.setDataAnalise(LocalDateTime.now());
        analysis.setDataValidade(LocalDateTime.now().plusMonths(6)); // Válido por 6 meses

        // Calcular decisão automaticamente
        AnalysisDecision decisao = calcularDecisao(analysis);
        analysis.setDecisao(decisao);

        // Calcular limite sugerido
        BigDecimal limiteSugerido = calcularLimiteSugerido(analysis);
        analysis.setLimiteAprovado(limiteSugerido);

        // Calcular taxa de deságio sugerida
        BigDecimal taxaSugerida = calcularTaxaDesagio(analysis);
        analysis.setTaxaDesagioSugerida(taxaSugerida);

        client.setAnaliseCredito(analysis);

        // Atualizar status do cliente
        if (decisao == AnalysisDecision.APROVADO) {
            client.setStatus(ClientStatus.APROVADO);
            client.setLimiteCredito(limiteSugerido);
            client.setLimiteDisponivel(limiteSugerido);
        } else if (decisao == AnalysisDecision.REPROVADO) {
            client.setStatus(ClientStatus.REPROVADO);
            client.setLimiteCredito(BigDecimal.ZERO);
            client.setLimiteDisponivel(BigDecimal.ZERO);
        } else {
            client.setStatus(ClientStatus.APROVADO);
            client.setLimiteCredito(limiteSugerido);
            client.setLimiteDisponivel(limiteSugerido);
        }

        clientRepository.save(client);

        log.info("Análise de crédito realizada para cliente ID {}: {} - Limite: {}",
                clientId, decisao, limiteSugerido);

        return converterParaDTO(analysis);
    }

    /**
     * Realiza análise automática baseada em regras
     */
    @Transactional
    public CreditAnalysisDTO realizarAnaliseAutomatica(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        CreditAnalysis analysis = new CreditAnalysis();
        analysis.setClient(client);

        // Aqui você integraria com APIs de bureaus de crédito
        // Por enquanto, vamos usar valores simulados
        analysis.setScoreProprio(calcularScoreProprio(client));
        analysis.setScoreSerasa(0); // Seria obtido via API
        analysis.setQuantidadeProtestos(0); // Seria obtido via API
        analysis.setTemRestricaoCredito(false); // Seria obtido via API
        analysis.setDataAnalise(LocalDateTime.now());
        analysis.setDataValidade(LocalDateTime.now().plusMonths(6));
        analysis.setAnalistaNome("Sistema Automático");

        // Decisão automática
        AnalysisDecision decisao = calcularDecisao(analysis);
        analysis.setDecisao(decisao);

        BigDecimal limiteSugerido = calcularLimiteSugerido(analysis);
        analysis.setLimiteAprovado(limiteSugerido);

        BigDecimal taxaSugerida = calcularTaxaDesagio(analysis);
        analysis.setTaxaDesagioSugerida(taxaSugerida);

        client.setAnaliseCredito(analysis);
        client.setStatus(ClientStatus.EM_ANALISE);

        clientRepository.save(client);

        log.info("Análise automática realizada para cliente ID {}", clientId);

        return converterParaDTO(analysis);
    }

    /**
     * Busca análise de crédito de um cliente
     */
    public CreditAnalysisDTO buscarAnalise(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        if (client.getAnaliseCredito() == null) {
            throw new RuntimeException("Cliente não possui análise de crédito");
        }

        return converterParaDTO(client.getAnaliseCredito());
    }

    /**
     * Verifica se a análise de crédito está válida
     */
    public boolean isAnaliseValida(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        CreditAnalysis analysis = client.getAnaliseCredito();

        if (analysis == null || analysis.getDataValidade() == null) {
            return false;
        }

        return LocalDateTime.now().isBefore(analysis.getDataValidade());
    }

    // ========== Métodos Privados de Cálculo ==========

    private AnalysisDecision calcularDecisao(CreditAnalysis analysis) {
        int pontos = 0;

        // Score próprio (0-40 pontos)
        if (analysis.getScoreProprio() != null) {
            if (analysis.getScoreProprio() >= 800) pontos += 40;
            else if (analysis.getScoreProprio() >= 600) pontos += 30;
            else if (analysis.getScoreProprio() >= 400) pontos += 20;
            else if (analysis.getScoreProprio() >= 200) pontos += 10;
        }

        // Faturamento (0-30 pontos)
        if (analysis.getFaturamentoMensal() != null) {
            BigDecimal faturamento = analysis.getFaturamentoMensal();
            if (faturamento.compareTo(new BigDecimal("1000000")) >= 0) pontos += 30;
            else if (faturamento.compareTo(new BigDecimal("500000")) >= 0) pontos += 25;
            else if (faturamento.compareTo(new BigDecimal("100000")) >= 0) pontos += 20;
            else if (faturamento.compareTo(new BigDecimal("50000")) >= 0) pontos += 15;
            else if (faturamento.compareTo(new BigDecimal("10000")) >= 0) pontos += 10;
        }

        // Restrições (penalidades)
        if (Boolean.TRUE.equals(analysis.getTemRestricaoCredito())) {
            pontos -= 30;
        }

        if (analysis.getQuantidadeProtestos() != null && analysis.getQuantidadeProtestos() > 0) {
            pontos -= (analysis.getQuantidadeProtestos() * 10);
        }

        // Decisão baseada em pontos
        if (pontos >= 60) {
            return AnalysisDecision.APROVADO;
        } else if (pontos >= 40) {
            return AnalysisDecision.APROVADO_COM_RESTRICAO;
        } else {
            return AnalysisDecision.REPROVADO;
        }
    }

    private BigDecimal calcularLimiteSugerido(CreditAnalysis analysis) {
        if (analysis.getDecisao() == AnalysisDecision.REPROVADO) {
            return BigDecimal.ZERO;
        }

        BigDecimal limiteBase = BigDecimal.ZERO;

        // Limite baseado no faturamento (30% do faturamento mensal)
        if (analysis.getFaturamentoMensal() != null) {
            limiteBase = analysis.getFaturamentoMensal()
                    .multiply(new BigDecimal("0.30"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Ajustar baseado na decisão
        if (analysis.getDecisao() == AnalysisDecision.APROVADO_COM_RESTRICAO) {
            limiteBase = limiteBase.multiply(new BigDecimal("0.50")); // 50% do limite normal
        }

        // Limite mínimo e máximo
        BigDecimal limiteMinimo = new BigDecimal("10000");
        BigDecimal limiteMaximo = new BigDecimal("5000000");

        if (limiteBase.compareTo(limiteMinimo) < 0) {
            limiteBase = limiteMinimo;
        }
        if (limiteBase.compareTo(limiteMaximo) > 0) {
            limiteBase = limiteMaximo;
        }

        return limiteBase;
    }

    private BigDecimal calcularTaxaDesagio(CreditAnalysis analysis) {
        // Taxa base: 1.5% ao mês
        BigDecimal taxaBase = new BigDecimal("1.50");

        // Ajustar baseado no score
        if (analysis.getScoreProprio() != null) {
            if (analysis.getScoreProprio() >= 800) {
                taxaBase = new BigDecimal("1.20"); // Cliente excelente
            } else if (analysis.getScoreProprio() >= 600) {
                taxaBase = new BigDecimal("1.50"); // Cliente bom
            } else if (analysis.getScoreProprio() >= 400) {
                taxaBase = new BigDecimal("2.00"); // Cliente regular
            } else {
                taxaBase = new BigDecimal("3.00"); // Cliente arriscado
            }
        }

        // Ajustar por restrições
        if (Boolean.TRUE.equals(analysis.getTemRestricaoCredito())) {
            taxaBase = taxaBase.add(new BigDecimal("0.50"));
        }

        if (analysis.getQuantidadeProtestos() != null && analysis.getQuantidadeProtestos() > 0) {
            taxaBase = taxaBase.add(new BigDecimal("0.30"));
        }

        return taxaBase.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer calcularScoreProprio(Client client) {
        // Score baseado em tempo de cadastro, atividade, etc.
        // Por enquanto, retorna um score médio
        return 500;
    }

    private CreditAnalysisDTO converterParaDTO(CreditAnalysis analysis) {
        if (analysis == null) {
            return null;
        }

        boolean valida = analysis.getDataValidade() != null &&
                LocalDateTime.now().isBefore(analysis.getDataValidade());

        Integer diasParaVencimento = null;
        if (analysis.getDataValidade() != null) {
            diasParaVencimento = (int) ChronoUnit.DAYS.between(
                    LocalDateTime.now(), analysis.getDataValidade()
            );
        }

        return CreditAnalysisDTO.builder()
                .id(analysis.getId())
                .clientId(analysis.getClient().getId())
                .scoreProprio(analysis.getScoreProprio())
                .scoreSerasa(analysis.getScoreSerasa())
                .faturamentoMensal(analysis.getFaturamentoMensal())
                .patrimonioLiquido(analysis.getPatrimonioLiquido())
                .margemLucro(analysis.getMargemLucro())
                .quantidadeProtestos(analysis.getQuantidadeProtestos())
                .temRestricaoCredito(analysis.getTemRestricaoCredito())
                .observacoes(analysis.getObservacoes())
                .decisao(analysis.getDecisao() != null ? analysis.getDecisao().name() : null)
                .limiteAprovado(analysis.getLimiteAprovado())
                .taxaDesagioSugerida(analysis.getTaxaDesagioSugerida())
                .dataAnalise(analysis.getDataAnalise())
                .analistaNome(analysis.getAnalistaNome())
                .dataValidade(analysis.getDataValidade())
                .valida(valida)
                .diasParaVencimento(diasParaVencimento)
                .build();
    }
}