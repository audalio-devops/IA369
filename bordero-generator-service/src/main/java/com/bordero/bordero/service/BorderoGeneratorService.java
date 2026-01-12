package com.bordero.bordero.service;

import com.bordero.bordero.client.ClientServiceClient;
import com.bordero.bordero.domain.model.*;
import com.bordero.bordero.repository.BorderoRepository;
import com.bordero.bordero.repository.TipoTituloRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BorderoGeneratorService {

    private final BorderoRepository repository;
    private final NFeClientService nfeClientService;
    private final TarifaService tarifaService;
    private final DiaUtilService diaUtilService;
    private final ClientServiceClient clientServiceClient;
    private final TipoTituloRepository tipoTituloRepository;
    private final PDFBorderoService pdfBorderoService;

    private static final int FLOAT_BASE = 2; // D+2 padrão

    @KafkaListener(topics = "nfe-events", groupId = "bordero-generator")
    public void processarEventoNFe(Map<String, Object> evento) {
        String tipo = (String) evento.get("tipo");

        if ("NFE_PROCESSADA".equals(tipo)) {
            Long nfeId = ((Number) evento.get("nfeId")).longValue();
            gerarBorderoAutomatico(nfeId);
        }
    }

    /**
     * NOVO MÉTODO: Gera borderô com múltiplas NFes
     * Este é o método principal para atender à nova especificação
     */
    @Transactional
    public Bordero gerarBorderoComMultiplasNFes(String cnpjCliente, List<Long> nfeIds) {
        log.info("Gerando borderô para cliente {} com {} NFes", cnpjCliente, nfeIds.size());

        if (nfeIds == null || nfeIds.isEmpty()) {
            throw new IllegalArgumentException("Lista de NFes não pode ser vazia");
        }

        // Buscar ou criar tipo de título NF
        TipoTitulo tipoNF = tipoTituloRepository.findByTipo("NF")
                .orElseGet(() -> criarTipoTituloNF());

        // Criar borderô
        Bordero bordero = Bordero.builder()
                .dataGeracao(LocalDateTime.now())
                .cnpjCliente(cnpjCliente)
                .cnpjFundo("09609468000152")
                .nomeFundo("F.I.D.C. MACRO FUND")
                .status(StatusBordero.GERADO)
                .build();

        // Variáveis para totalização
        BigDecimal valorTotalBruto = BigDecimal.ZERO;
        BigDecimal valorTotalDesagio = BigDecimal.ZERO;
        BigDecimal valorTotalLiquido = BigDecimal.ZERO;
        int somaDiasCorridos = 0;
        int somaDiasUteis = 0;
        LocalDateTime menorVenc = null;
        LocalDateTime maiorVenc = null;
        Set<String> sacadosUnicos = new HashSet<>();

        // Processar cada NFe
        for (Long nfeId : nfeIds) {
            Map<String, Object> nfeData = nfeClientService.buscarNFe(nfeId);

            // Atualizar dados do cedente (primeira NFe define)
            if (bordero.getCnpjCedente() == null) {
                bordero.setCnpjCedente(extrairCnpj(nfeData));
                bordero.setNomeCedente(extrairRazaoSocial(nfeData));
            }

            // Processar duplicatas desta NFe
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> duplicatas =
                    (List<Map<String, Object>>) nfeData.get("duplicatas");

            if (duplicatas == null || duplicatas.isEmpty()) {
                log.warn("NFe ID {} não possui duplicatas, pulando", nfeId);
                continue;
            }

            LocalDate dataHoje = LocalDate.now();

            for (Map<String, Object> dup : duplicatas) {
                String numeroDuplicata = dup.get("numero").toString();
                String vencimentoStr = dup.get("vencimento").toString();
                LocalDateTime vencimento = LocalDateTime.parse(vencimentoStr);

                BigDecimal valorOriginal = new BigDecimal(dup.get("valor").toString());
                BigDecimal valor = valorOriginal.abs();

                if (valorOriginal.compareTo(BigDecimal.ZERO) < 0) {
                    log.warn("Valor negativo detectado na duplicata {}: {} - convertido para positivo",
                            numeroDuplicata, valorOriginal);
                }

                Long duplicataId = Long.valueOf(dup.get("id") != null ? dup.get("id").toString() : "0");

                // Calcular float (D+) considerando fins de semana e feriados
                LocalDate dataVencimento = vencimento.toLocalDate();
                LocalDate dataCompensacao = diaUtilService.calcularProximoDiaUtil(
                        dataVencimento, FLOAT_BASE, "SP", null
                );

                // Calcular D+ (float)
                int floatDias = (int) ChronoUnit.DAYS.between(dataVencimento, dataCompensacao);

                // Dias corridos até a data de compensação
                int diasCorridos = (int) ChronoUnit.DAYS.between(dataHoje, dataCompensacao);

                if (diasCorridos < 0) {
                    diasCorridos = 0;
                    log.warn("Duplicata {} já vencida. Dias corridos ajustado para 0", numeroDuplicata);
                }

                // Dias úteis
                int diasUteis = diaUtilService.calcularDiasUteis(dataHoje, dataCompensacao, "SP", null);

                // Calcular deságio usando FATOR
                // Fórmula: Deságio = VLR_BRUTO × ((FATOR / DIAS_VCTO) × (DIAS_VCTO + D+))
                BigDecimal fator = new BigDecimal("0.0175"); // 1.75% a.m.
                BigDecimal desagio = BigDecimal.ZERO;

                if (diasCorridos > 0) {
                    BigDecimal diasVcto = new BigDecimal(diasCorridos);
                    BigDecimal diasFloat = new BigDecimal(floatDias);
                    BigDecimal diasTotal = diasVcto.add(diasFloat);

                    // (FATOR / diasCorridos) * diasTotal
                    BigDecimal taxaDiaria = fator.divide(diasVcto, 10, RoundingMode.HALF_UP);
                    desagio = valor
                            .multiply(taxaDiaria)
                            .multiply(diasTotal)
                            .setScale(2, RoundingMode.HALF_UP);
                }

                if (desagio.compareTo(BigDecimal.ZERO) < 0) {
                    desagio = BigDecimal.ZERO;
                }

                // Valor líquido inicial (sem tarifas)
                BigDecimal valorLiquido = valor.subtract(desagio);

                if (valorLiquido.compareTo(BigDecimal.ZERO) < 0) {
                    log.error("Valor líquido negativo detectado! Valor: {}, Deságio: {}",
                            valor, desagio);
                    valorLiquido = BigDecimal.ZERO;
                }

                String cnpjSacado = extrairDadoDeLista(nfeData, "destinatario", "cnpj");
                String nomeSacado = extrairDadoDeLista(nfeData, "destinatario", "razaoSocial");

                // Adicionar à lista de sacados únicos
                sacadosUnicos.add(cnpjSacado);

                TituloBordero titulo = TituloBordero.builder()
                        .tipoTitulo(tipoNF)
                        .nfeId(nfeId)
                        .duplicataId(duplicataId)
                        .chaveAcessoNFe(nfeData.get("chaveAcesso").toString())
                        .numeroNFe(nfeData.get("numeroNfe").toString())
                        .numeroDuplicata(numeroDuplicata)
                        .dataVencimento(vencimento)
                        .valorBruto(valor)
                        .diasParaVencimento(diasCorridos)
                        .diasUteis(diasUteis)
                        .prazoAdicional(0) // PZ sempre 0 por enquanto
                        .floatDias(floatDias) // D+
                        .dataCompensacao(dataCompensacao)
                        .taxaDesagio(fator.multiply(new BigDecimal("100"))) // Percentual
                        .valorDesagio(desagio)
                        .valorLiquido(valorLiquido)
                        .cnpjSacado(cnpjSacado)
                        .nomeSacado(nomeSacado)
                        .cnpjEmitente(extrairCnpj(nfeData))
                        .nomeEmitente(extrairRazaoSocial(nfeData))
                        .build();

                bordero.addTitulo(titulo);

                valorTotalBruto = valorTotalBruto.add(valor);
                valorTotalDesagio = valorTotalDesagio.add(desagio);
                valorTotalLiquido = valorTotalLiquido.add(valorLiquido);
                somaDiasCorridos += diasCorridos;
                somaDiasUteis += diasUteis;

                if (menorVenc == null || vencimento.isBefore(menorVenc)) {
                    menorVenc = vencimento;
                }
                if (maiorVenc == null || vencimento.isAfter(maiorVenc)) {
                    maiorVenc = vencimento;
                }

                log.debug("Título processado: {} - Valor Bruto: {} - Deságio: {} - Líquido: {} - D+: {}",
                        numeroDuplicata, valor, desagio, valorLiquido, floatDias);
            }
        }

        int quantidadeTitulosProcessados = bordero.getTitulos().size();

        if (quantidadeTitulosProcessados == 0) {
            throw new IllegalStateException("Nenhum título foi processado para o borderô");
        }

        // Calcular tarifas
        // Taxa Serasa: R$ 50,00 por sacado único
        int quantidadeSacados = sacadosUnicos.size();
        boolean incluirSerasa = quantidadeSacados > 0;

        CalculoTarifasResult tarifas = tarifaService.calcularTarifas(
                quantidadeTitulosProcessados,
                incluirSerasa
        );

        // Aplicar tarifas por documento
        BigDecimal tarifaPorTitulo = tarifas.getTarifasPorDocumento()
                .divide(new BigDecimal(quantidadeTitulosProcessados), 2, RoundingMode.HALF_UP);

        for (TituloBordero titulo : bordero.getTitulos()) {
            BigDecimal liquidoAtual = titulo.getValorLiquido();
            BigDecimal novoLiquido = liquidoAtual.subtract(tarifaPorTitulo);

            if (novoLiquido.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Valor líquido do título {} ficaria negativo após tarifa. Ajustando para zero",
                        titulo.getNumeroDuplicata());
                novoLiquido = BigDecimal.ZERO;
            }

            titulo.setValorLiquido(novoLiquido);
            titulo.setTarifaDocumento(tarifaPorTitulo);
        }

        // Recalcular líquido total após tarifas
        valorTotalLiquido = valorTotalLiquido.subtract(tarifas.getTarifasPorDocumento());

        // Aplicar tarifas de cliente e gerais
        BigDecimal valorFinal = valorTotalLiquido
                .subtract(tarifas.getTarifasPorCliente())
                .subtract(tarifas.getTarifasGerais());

        if (valorFinal.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Valor final do borderô ficaria negativo! Ajustando para zero.");
            valorFinal = BigDecimal.ZERO;
        }

        // Preencher borderô
        bordero.setValorBruto(valorTotalBruto);
        bordero.setValorDesagio(valorTotalDesagio);
        bordero.setValorTarifas(tarifas.getValorTotal());
        bordero.setTarifasDetalhamento(tarifas.getDetalhamento().toString());
        bordero.setValorLiquido(valorFinal);

        bordero.setQuantidadeTitulos(quantidadeTitulosProcessados);
        bordero.setQuantidadeSacados(quantidadeSacados);
        bordero.setPrazoMedio(somaDiasCorridos / quantidadeTitulosProcessados);
        bordero.setPrazoMedioDiasUteis(somaDiasUteis / quantidadeTitulosProcessados);
        bordero.setVencimentoMenor(menorVenc);
        bordero.setVencimentoMaior(maiorVenc);

        Bordero borderoSalvo = repository.save(bordero);

        log.info("Borderô gerado com sucesso: {} | NFes: {} | Títulos: {} | Sacados: {} | Valor Bruto: R$ {} | Valor Líquido: R$ {}",
                borderoSalvo.getNumeroBordero(),
                nfeIds.size(),
                borderoSalvo.getQuantidadeTitulos(),
                quantidadeSacados,
                borderoSalvo.getValorBruto(),
                borderoSalvo.getValorLiquido());

        return borderoSalvo;
    }

    /**
     * Método original mantido para compatibilidade com Kafka
     */
    @Transactional
    public Bordero gerarBorderoAutomatico(Long nfeId) {
        // Chama o novo método com apenas uma NFe
        return gerarBorderoComMultiplasNFes(null, List.of(nfeId));
    }

    /**
     * Busca borderô por ID
     */
    public Bordero buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Borderô não encontrado"));
    }

    /**
     * Lista borderôs por status
     */
    public List<Bordero> listarPorStatus(String status) {
        if (status == null || status.isEmpty()) {
            return repository.findAll();
        }
        // Implementar filtro por status no repository se necessário
        return repository.findAll().stream()
                .filter(b -> b.getStatus().name().equals(status))
                .collect(Collectors.toList());
    }

    /**
     * Gera PDF do borderô
     */
    public byte[] gerarPDFBordero(Long borderoId) {
        Bordero bordero = buscarPorId(borderoId);
        return pdfBorderoService.gerarPDF(bordero);
    }

    /**
     * Cria tipo de título NF se não existir
     */
    private TipoTitulo criarTipoTituloNF() {
        TipoTitulo tipoNF = TipoTitulo.builder()
                .tipo("NF")
                .nome("Nota Fiscal")
                .descricao("Título existente em uma nota fiscal")
                .ativo(true)
                .build();
        return tipoTituloRepository.save(tipoNF);
    }

    // Métodos auxiliares mantidos
    private String extrairCnpj(Map<String, Object> nfeData) {
        return extrairDadoDoEmitente(nfeData, "cnpj");
    }

    private String extrairRazaoSocial(Map<String, Object> nfeData) {
        return extrairDadoDoEmitente(nfeData, "razaoSocial");
    }

    @SuppressWarnings("unchecked")
    private String extrairDadoDoEmitente(Map<String, Object> nfeData, String chaveBusca) {
        if (nfeData == null) return null;
        Object emitenteObj = nfeData.get("emitente");
        if (emitenteObj instanceof Map) {
            Map<String, Object> emitenteMap = (Map<String, Object>) emitenteObj;
            Object valor = emitenteMap.get(chaveBusca);
            return valor != null ? valor.toString() : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extrairDadoDeLista(Map<String, Object> nfeData, String nomeLista, String chaveBusca) {
        if (nfeData == null) return null;
        Object listaObj = nfeData.get(nomeLista);
        if (listaObj instanceof Map) {
            Map<String, Object> listaMap = (Map<String, Object>) listaObj;
            Object valor = listaMap.get(chaveBusca);
            return valor != null ? valor.toString() : null;
        }
        return null;
    }
}