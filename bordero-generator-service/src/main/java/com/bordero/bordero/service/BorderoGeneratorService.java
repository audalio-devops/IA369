package com.bordero.bordero.service;

import com.bordero.bordero.domain.model.*;
import com.bordero.bordero.repository.BorderoRepository;
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

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class BorderoGeneratorService {

    private final BorderoRepository repository;
    private final NFeClientService nfeClientService;
    private final TarifaService tarifaService;
    private final DiaUtilService diaUtilService;

    private static final int FLOAT_BASE = 2; // D+2 padrão

    @KafkaListener(topics = "nfe-events", groupId = "bordero-generator")
    public void processarEventoNFe(Map<String, Object> evento) {
        String tipo = (String) evento.get("tipo");

        if ("NFE_PROCESSADA".equals(tipo)) {
            Long nfeId = ((Number) evento.get("nfeId")).longValue();
            gerarBorderoAutomatico(nfeId);
        }
    }

    @Transactional
    public Bordero gerarBorderoAutomatico(Long nfeId) {
        log.info("Gerando borderô para NF-e ID: {}", nfeId);

        // Buscar dados da NF-e via Feign Client
        Map<String, Object> nfeData = nfeClientService.buscarNFe(nfeId);

        Bordero bordero = Bordero.builder()
                .dataGeracao(LocalDateTime.now())
                .cnpjCedente(extrairCnpj(nfeData))
                .nomeCedente(extrairRazaoSocial(nfeData))
                .cnpjFundo("09609468000152")
                .nomeFundo("F.I.D.C. MACRO FUND")
                .status(StatusBordero.GERADO)
                .build();

        // Buscar duplicatas
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> duplicatas =
                (List<Map<String, Object>>) nfeData.get("duplicatas");

        if (duplicatas == null || duplicatas.isEmpty()) {
            throw new IllegalArgumentException("NFe não possui duplicatas para gerar borderô");
        }

        // Controle de duplicatas já processadas
        Set<String> numerosProcessados = new LinkedHashSet<>();

        BigDecimal valorTotalBruto = BigDecimal.ZERO;
        BigDecimal valorTotalDesagio = BigDecimal.ZERO;
        BigDecimal valorTotalLiquido = BigDecimal.ZERO;
        int somaDiasCorridos = 0;
        int somaDiasUteis = 0;
        LocalDateTime menorVenc = null;
        LocalDateTime maiorVenc = null;

        LocalDate dataHoje = LocalDate.now();

        for (Map<String, Object> dup : duplicatas) {
            String numeroDuplicata = dup.get("numero").toString();

            // ========== CORREÇÃO 1: Evitar duplicatas ==========
            if (numerosProcessados.contains(numeroDuplicata)) {
                log.warn("Duplicata {} já foi processada, ignorando", numeroDuplicata);
                continue;
            }
            numerosProcessados.add(numeroDuplicata);

            String vencimentoStr = dup.get("vencimento").toString();
            LocalDateTime vencimento = LocalDateTime.parse(vencimentoStr);

            // ========== CORREÇÃO 2: Garantir valor sempre positivo ==========
            BigDecimal valorOriginal = new BigDecimal(dup.get("valor").toString());
            BigDecimal valor = valorOriginal.abs(); // Sempre positivo

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

            // Dias corridos até a data de compensação
            int diasCorridos = (int) ChronoUnit.DAYS.between(dataHoje, dataCompensacao);

            // Garantir que dias corridos não seja negativo
            if (diasCorridos < 0) {
                diasCorridos = 0;
                log.warn("Duplicata {} já vencida. Dias corridos ajustado para 0", numeroDuplicata);
            }

            // Dias úteis (para informação)
            int diasUteis = diaUtilService.calcularDiasUteis(dataHoje, dataCompensacao, "SP", null);

            // Calcular taxa de deságio (mensal)
            // Exemplo: 1.5% ao mês = 0.05% ao dia
            BigDecimal taxaMensal = new BigDecimal("1.50"); // 1.5% a.m.
            BigDecimal taxaDiaria = taxaMensal.divide(new BigDecimal("30"), 6, RoundingMode.HALF_UP);

            // Deságio = Valor * Taxa Diária * Dias Corridos / 100
            BigDecimal desagio = valor
                    .multiply(taxaDiaria)
                    .multiply(new BigDecimal(diasCorridos))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            // ========== CORREÇÃO 3: Garantir deságio não negativo ==========
            if (desagio.compareTo(BigDecimal.ZERO) < 0) {
                desagio = BigDecimal.ZERO;
                log.warn("Deságio negativo detectado, ajustado para zero");
            }

            // Valor líquido = Valor bruto - Deságio
            BigDecimal valorLiquido = valor.subtract(desagio);

            // ========== CORREÇÃO 4: Garantir valor líquido não negativo ==========
            if (valorLiquido.compareTo(BigDecimal.ZERO) < 0) {
                log.error("Valor líquido negativo detectado! Valor: {}, Deságio: {}",
                        valor, desagio);
                valorLiquido = BigDecimal.ZERO;
            }

            TituloBordero titulo = TituloBordero.builder()
                    .nfeId(nfeId)
                    .duplicataId(duplicataId)
                    .chaveAcessoNFe(nfeData.get("chaveAcesso").toString())
                    .numeroNFe(nfeData.get("numeroNfe").toString())
                    .numeroDuplicata(numeroDuplicata)
                    .dataVencimento(vencimento)
                    .valorBruto(valor) // Sempre positivo
                    .diasParaVencimento(diasCorridos)
                    .diasUteis(diasUteis)
                    .dataCompensacao(dataCompensacao)
                    .taxaDesagio(taxaMensal)
                    .valorDesagio(desagio) // Sempre positivo ou zero
                    .valorLiquido(valorLiquido) // Sempre positivo ou zero
                    .cnpjSacado(extrairDadoDeLista(nfeData,"destinatario", "cnpj"))
                    .nomeSacado(extrairDadoDeLista(nfeData,"destinatario", "razaoSocial"))
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

            log.debug("Título processado: {} - Valor Bruto: {} - Deságio: {} - Líquido: {}",
                    numeroDuplicata, valor, desagio, valorLiquido);
        }

        int quantidadeTitulosProcessados = bordero.getTitulos().size();

        if (quantidadeTitulosProcessados == 0) {
            throw new IllegalStateException("Nenhum título foi processado para o borderô");
        }

        // Calcular tarifas
        CalculoTarifasResult tarifas = tarifaService.calcularTarifas(
                quantidadeTitulosProcessados,
                true // Incluir consulta Serasa
        );

        // Aplicar tarifas por documento (subtrair do líquido de cada título)
        BigDecimal tarifaPorTitulo = tarifas.getTarifasPorDocumento()
                .divide(new BigDecimal(quantidadeTitulosProcessados), 2, RoundingMode.HALF_UP);

        for (TituloBordero titulo : bordero.getTitulos()) {
            BigDecimal liquidoAtual = titulo.getValorLiquido();
            BigDecimal novoLiquido = liquidoAtual.subtract(tarifaPorTitulo);

            // Garantir que não fique negativo após tarifa
            if (novoLiquido.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Valor líquido do título {} ficaria negativo após tarifa. Ajustando para zero",
                        titulo.getNumeroDuplicata());
                novoLiquido = BigDecimal.ZERO;
            }

            titulo.setValorLiquido(novoLiquido);
            titulo.setTarifaDocumento(tarifaPorTitulo);
        }

        // Recalcular líquido total após tarifas por documento
        valorTotalLiquido = valorTotalLiquido.subtract(tarifas.getTarifasPorDocumento());

        // Aplicar tarifas de cliente e gerais no total
        BigDecimal valorFinal = valorTotalLiquido
                .subtract(tarifas.getTarifasPorCliente())
                .subtract(tarifas.getTarifasGerais());

        // Garantir que valor final não seja negativo
        if (valorFinal.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Valor final do borderô ficaria negativo! Ajustando para zero. " +
                    "Verifique as tarifas configuradas.");
            valorFinal = BigDecimal.ZERO;
        }

        // Preencher borderô
        bordero.setValorBruto(valorTotalBruto);
        bordero.setValorDesagio(valorTotalDesagio);
        bordero.setValorTarifas(tarifas.getValorTotal());
        bordero.setTarifasDetalhamento(tarifas.getDetalhamento().toString());
        bordero.setValorLiquido(valorFinal);

        bordero.setQuantidadeTitulos(quantidadeTitulosProcessados);
        bordero.setPrazoMedio(somaDiasCorridos / quantidadeTitulosProcessados);
        bordero.setPrazoMedioDiasUteis(somaDiasUteis / quantidadeTitulosProcessados);
        bordero.setVencimentoMenor(menorVenc);
        bordero.setVencimentoMaior(maiorVenc);

        Bordero borderoSalvo = repository.save(bordero);

        log.info("Borderô gerado com sucesso: {} | Títulos: {} | Valor Bruto: R$ {} | Valor Líquido: R$ {}",
                borderoSalvo.getNumeroBordero(),
                borderoSalvo.getQuantidadeTitulos(),
                borderoSalvo.getValorBruto(),
                borderoSalvo.getValorLiquido());

        return borderoSalvo;
    }

    public byte[] gerarPDFBordero(Long borderoId) {
        Bordero bordero = repository.findById(borderoId)
                .orElseThrow(() -> new RuntimeException("Borderô não encontrado"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Fontes
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font fontCabecalhoTabela = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

            // Formatadores
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            DateTimeFormatter dataFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // 1. Título
            Paragraph titulo = new Paragraph("BORDERÔ DE DESCONTO DE RECEBÍVEIS", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            document.add(titulo);

            // 2. Dados do Cedente e Fundo
            PdfPTable dadosTable = new PdfPTable(2);
            dadosTable.setWidthPercentage(100);
            dadosTable.setSpacingAfter(20);

            dadosTable.addCell(getCell("CEDENTE:", fontSubtitulo));
            dadosTable.addCell(getCell("FUNDO (Cessionário):", fontSubtitulo));
            dadosTable.addCell(getCell(bordero.getNomeCedente() + "\nCNPJ: " + bordero.getCnpjCedente(), fontNormal));
            dadosTable.addCell(getCell(bordero.getNomeFundo() + "\nCNPJ: " + bordero.getCnpjFundo(), fontNormal));

            document.add(dadosTable);

            // 3. Resumo Financeiro (Box)
            PdfPTable resumoTable = new PdfPTable(4);
            resumoTable.setWidthPercentage(100);
            resumoTable.setSpacingAfter(20);

            // Cabeçalhos do Resumo
            addHeaderCell(resumoTable, "Valor Bruto", fontCabecalhoTabela);
            addHeaderCell(resumoTable, "Deságio Total", fontCabecalhoTabela);
            addHeaderCell(resumoTable, "Tarifas", fontCabecalhoTabela);
            addHeaderCell(resumoTable, "VALOR LÍQUIDO", fontCabecalhoTabela);

            // Valores do Resumo
            resumoTable.addCell(getCell(currencyFormat.format(bordero.getValorBruto()), fontNormal));
            resumoTable.addCell(getCell(currencyFormat.format(bordero.getValorDesagio()), fontNormal));
            resumoTable.addCell(getCell(currencyFormat.format(bordero.getValorTarifas()), fontNormal));

            // Destaque para o valor líquido
            Font fontLiquido = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            resumoTable.addCell(getCell(currencyFormat.format(bordero.getValorLiquido()), fontLiquido));

            document.add(resumoTable);

            document.add(new Paragraph("Número do Borderô: " + bordero.getNumeroBordero(), fontNormal));
            document.add(new Paragraph("Data de Geração: " + bordero.getDataGeracao().format(dataFormatter), fontNormal));
            document.add(new Paragraph(" ", fontNormal)); // Espaço

            // 4. Lista de Títulos (Tabela Detalhada)
            document.add(new Paragraph("DETALHAMENTO DOS TÍTULOS", fontSubtitulo));
            document.add(new Paragraph(" ", fontNormal));

            PdfPTable titulosTable = new PdfPTable(6); // 6 Colunas
            titulosTable.setWidthPercentage(100);
            titulosTable.setWidths(new float[]{2, 2, 2, 2, 1, 2}); // Largura relativa das colunas

            addHeaderCell(titulosTable, "NF-e / Duplicata", fontCabecalhoTabela);
            addHeaderCell(titulosTable, "Sacado", fontCabecalhoTabela);
            addHeaderCell(titulosTable, "Vencimento", fontCabecalhoTabela);
            addHeaderCell(titulosTable, "Valor Bruto", fontCabecalhoTabela);
            addHeaderCell(titulosTable, "Dias", fontCabecalhoTabela);
            addHeaderCell(titulosTable, "Valor Líquido", fontCabecalhoTabela);

            for (TituloBordero tituloBord : bordero.getTitulos()) {
                titulosTable.addCell(getCell(tituloBord.getNumeroNFe() + " / " + tituloBord.getNumeroDuplicata(), fontNormal));
                titulosTable.addCell(getCell(tituloBord.getNomeSacado(), fontNormal));
                titulosTable.addCell(getCell(tituloBord.getDataVencimento().format(dataFormatter), fontNormal));
                titulosTable.addCell(getCell(currencyFormat.format(tituloBord.getValorBruto()), fontNormal));
                titulosTable.addCell(getCell(String.valueOf(tituloBord.getDiasParaVencimento()), fontNormal));
                titulosTable.addCell(getCell(currencyFormat.format(tituloBord.getValorLiquido()), fontNormal));
            }

            document.add(titulosTable);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar PDF", e);
            throw new RuntimeException("Erro ao gerar PDF do borderô", e);
        }
    }

    // Métodos auxiliares para formatar tabelas
    private PdfPCell getCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(Color.GRAY);
        return cell;
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(Color.DARK_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    /**
     * Método utilitário para extrair o CNPJ do Emitente de forma segura.
     */
    private String extrairCnpj(Map<String, Object> nfeData) {
        return extrairDadoDoEmitente(nfeData, "cnpj");
    }

    /**
     * Método utilitário para extrair a Razão Social do Emitente de forma segura.
     */
    private String extrairRazaoSocial(Map<String, Object> nfeData) {
        return extrairDadoDoEmitente(nfeData, "razaoSocial");
    }

    /**
     * Lógica comum para navegar no mapa "emitente" e buscar uma chave específica.
     */
    @SuppressWarnings("unchecked")
    private String extrairDadoDoEmitente(Map<String, Object> nfeData, String chaveBusca) {
        if (nfeData == null) return null;

        // 1. Obtém o objeto 'emitente'
        Object emitenteObj = nfeData.get("emitente");

        // 2. Verifica se ele é realmente um Mapa antes de tentar acessar
        if (emitenteObj instanceof Map) {
            Map<String, Object> emitenteMap = (Map<String, Object>) emitenteObj;

            // 3. Busca o valor (cnpj ou razaoSocial)
            Object valor = emitenteMap.get(chaveBusca);

            // 4. Retorna como String ou null se não existir
            return valor != null ? valor.toString() : null;
        }

        return null;
    }

    /**
     * Lógica comum para navegar no mapa "destinatario" e buscar uma chave específica.
     */
    @SuppressWarnings("unchecked")
    private String extrairDadoDeLista(Map<String, Object> nfeData, String nomeLista, String chaveBusca) {
        if (nfeData == null) return null;

        // 1. Obtém o objeto da lista (ex: 'destinatario')
        Object listaObj = nfeData.get(nomeLista);

        // 2. Verifica se ele é realmente um Mapa antes de tentar acessar
        if (listaObj instanceof Map) {
            Map<String, Object> listaMap = (Map<String, Object>) listaObj;

            // 3. Busca o valor (cnpj ou razaoSocial)
            Object valor = listaMap.get(chaveBusca);

            // 4. Retorna como String ou null se não existir
            return valor != null ? valor.toString() : null;
        }

        return null;
    }

}