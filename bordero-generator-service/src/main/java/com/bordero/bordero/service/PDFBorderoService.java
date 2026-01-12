package com.bordero.bordero.service;

import com.bordero.bordero.domain.model.Bordero;
import com.bordero.bordero.domain.model.TituloBordero;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Serviço responsável por gerar PDF do borderô conforme especificação
 * Formato: Orientação PAISAGEM
 * Colunas: DCTO | TIPO | VLR BRUTO | DT VCTO | PZ | D+ | DESÁGIO | VLR LÍQUIDO | SACADO
 */
@Service
@Slf4j
public class PDFBorderoService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(new Locale("pt", "BR"));

    public byte[] gerarPDF(Bordero bordero) {
        log.info("Gerando PDF para borderô {}", bordero.getNumeroBordero());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Documento em orientação PAISAGEM
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Fontes
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font fontPequena = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font fontCabecalho = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

            // === CABEÇALHO ===
            adicionarCabecalho(document, bordero, fontTitulo, fontNormal);

            // === TABELA DE TÍTULOS ===
            adicionarTabelaTitulos(document, bordero, fontCabecalho, fontPequena);

            // === RESUMO E ESTATÍSTICAS ===
            adicionarResumoEstatisticas(document, bordero, fontSubtitulo, fontNormal);

            document.close();
            log.info("PDF gerado com sucesso para borderô {}", bordero.getNumeroBordero());

            return out.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar PDF do borderô", e);
            throw new RuntimeException("Erro ao gerar PDF do borderô", e);
        }
    }

    private void adicionarCabecalho(Document document, Bordero bordero, Font fontTitulo, Font fontNormal) throws DocumentException {
        // Título
        Paragraph titulo = new Paragraph("BORDERÔ DE DESCONTO", fontTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(10);
        document.add(titulo);

        // Informações do borderô
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(15);

        infoTable.addCell(criarCelulaInfo("Número:", fontNormal, false));
        infoTable.addCell(criarCelulaInfo(bordero.getNumeroBordero(), fontNormal, true));
        infoTable.addCell(criarCelulaInfo("Data:", fontNormal, false));
        infoTable.addCell(criarCelulaInfo(bordero.getDataGeracao().format(DATETIME_FORMATTER), fontNormal, true));

        infoTable.addCell(criarCelulaInfo("Cedente:", fontNormal, false));
        infoTable.addCell(criarCelulaInfo(bordero.getCnpjCedente() + " - " + bordero.getNomeCedente(), fontNormal, true));
        infoTable.addCell(criarCelulaInfo("Status:", fontNormal, false));
        infoTable.addCell(criarCelulaInfo(bordero.getStatus().name(), fontNormal, true));

        if (bordero.getCnpjCliente() != null) {
            infoTable.addCell(criarCelulaInfo("Cliente:", fontNormal, false));
            infoTable.addCell(criarCelulaInfo(bordero.getCnpjCliente(), fontNormal, true));
            infoTable.addCell(criarCelulaInfo("Qtd. Títulos:", fontNormal, false));
            infoTable.addCell(criarCelulaInfo(String.valueOf(bordero.getQuantidadeTitulos()), fontNormal, true));
        }

        document.add(infoTable);
    }

    private void adicionarTabelaTitulos(Document document, Bordero bordero, Font fontCabecalho, Font fontPequena) throws DocumentException {
        // Colunas conforme especificação: DCTO | TIPO | VLR BRUTO | DT VCTO | PZ | D+ | DESÁGIO | VLR LÍQUIDO | SACADO
        float[] columnWidths = {12f, 5f, 10f, 9f, 4f, 4f, 10f, 10f, 30f};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);

        // Cabeçalho da tabela
        table.addCell(criarCelulaCabecalho("DCTO", fontCabecalho));
        table.addCell(criarCelulaCabecalho("TIPO", fontCabecalho));
        table.addCell(criarCelulaCabecalho("VLR BRUTO", fontCabecalho));
        table.addCell(criarCelulaCabecalho("DT VCTO", fontCabecalho));
        table.addCell(criarCelulaCabecalho("PZ", fontCabecalho));
        table.addCell(criarCelulaCabecalho("D+", fontCabecalho));
        table.addCell(criarCelulaCabecalho("DESÁGIO", fontCabecalho));
        table.addCell(criarCelulaCabecalho("VLR LÍQUIDO", fontCabecalho));
        table.addCell(criarCelulaCabecalho("SACADO", fontCabecalho));

        // Uma duplicata por linha
        for (TituloBordero titulo : bordero.getTitulos()) {
            // DCTO (número da NFe/Duplicata)
            table.addCell(criarCelulaDado(titulo.getNumeroNFe() + "/" + titulo.getNumeroDuplicata(), fontPequena, false));

            // TIPO
            String tipo = titulo.getTipoTitulo() != null ? titulo.getTipoTitulo().getTipo() : "NF";
            table.addCell(criarCelulaDado(tipo, fontPequena, true));

            // VLR BRUTO
            table.addCell(criarCelulaDado(CURRENCY_FORMAT.format(titulo.getValorBruto()), fontPequena, true));

            // DT VCTO
            table.addCell(criarCelulaDado(titulo.getDataVencimento().format(DATE_FORMATTER), fontPequena, true));

            // PZ (prazo adicional)
            int pz = titulo.getPrazoAdicional() != null ? titulo.getPrazoAdicional() : 0;
            table.addCell(criarCelulaDado(String.valueOf(pz), fontPequena, true));

            // D+ (float)
            int floatDias = titulo.getFloatDias() != null ? titulo.getFloatDias() : 0;
            table.addCell(criarCelulaDado(String.valueOf(floatDias), fontPequena, true));

            // DESÁGIO
            table.addCell(criarCelulaDado(CURRENCY_FORMAT.format(titulo.getValorDesagio()), fontPequena, true));

            // VLR LÍQUIDO
            table.addCell(criarCelulaDado(CURRENCY_FORMAT.format(titulo.getValorLiquido()), fontPequena, true));

            // SACADO (CNPJ - NOME)
            String sacado = formatarCNPJ(titulo.getCnpjSacado()) + " - " +
                    (titulo.getNomeSacado().length() > 30 ?
                            titulo.getNomeSacado().substring(0, 30) + "..." :
                            titulo.getNomeSacado());
            table.addCell(criarCelulaDado(sacado, fontPequena, false));
        }

        document.add(table);
    }

    private void adicionarResumoEstatisticas(Document document, Bordero bordero, Font fontSubtitulo, Font fontNormal) throws DocumentException {
        // Título da seção
        Paragraph tituloResumo = new Paragraph("INFORMAÇÕES DO BORDERÔ", fontSubtitulo);
        tituloResumo.setSpacingBefore(10);
        tituloResumo.setSpacingAfter(10);
        document.add(tituloResumo);

        // Tabela de resumo
        PdfPTable resumoTable = new PdfPTable(2);
        resumoTable.setWidthPercentage(60);
        resumoTable.setWidths(new float[]{2f, 1f});

        // Quantidade de títulos
        resumoTable.addCell(criarCelulaInfo("Quantidade de Títulos:", fontNormal, false));
        resumoTable.addCell(criarCelulaInfo(String.valueOf(bordero.getQuantidadeTitulos()), fontNormal, true));

        // Quantidade de sacados
        resumoTable.addCell(criarCelulaInfo("Quantidade de Sacados:", fontNormal, false));
        resumoTable.addCell(criarCelulaInfo(String.valueOf(bordero.getQuantidadeSacados()), fontNormal, true));

        // Valor Total Bruto
        resumoTable.addCell(criarCelulaInfo("Valor Total Bruto:", fontNormal, false));
        resumoTable.addCell(criarCelulaInfo(CURRENCY_FORMAT.format(bordero.getValorBruto()), fontNormal, true));

        // Valor Total Deságio
        resumoTable.addCell(criarCelulaInfo("Valor Total Deságio:", fontNormal, false));
        resumoTable.addCell(criarCelulaInfo(CURRENCY_FORMAT.format(bordero.getValorDesagio()), fontNormal, true));

        // Tarifas (detalhamento)
        BigDecimal tarifaTitulo = new BigDecimal("15.00");
        BigDecimal totalTarifasTitulos = tarifaTitulo.multiply(new BigDecimal(bordero.getQuantidadeTitulos()));
        resumoTable.addCell(criarCelulaInfo("Tarifa por Título (R$ 15,00 x " + bordero.getQuantidadeTitulos() + "):", fontNormal, false));
        resumoTable.addCell(criarCelulaInfo(CURRENCY_FORMAT.format(totalTarifasTitulos), fontNormal, true));

        // Taxa Serasa
        BigDecimal taxaSerasa = new BigDecimal("50.00").multiply(new BigDecimal(bordero.getQuantidadeSacados()));
        resumoTable.addCell(criarCelulaInfo("Taxa Consulta Serasa (R$ 50,00 x " + bordero.getQuantidadeSacados() + "):", fontNormal, false));
        resumoTable.addCell(criarCelulaInfo(CURRENCY_FORMAT.format(taxaSerasa), fontNormal, true));

        // VALOR LÍQUIDO TOTAL (destaque)
        Font fontDestaque = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell cellLabel = criarCelulaInfo("VALOR LÍQUIDO TOTAL:", fontDestaque, true);
        cellLabel.setBackgroundColor(new Color(51, 122, 183));
        resumoTable.addCell(cellLabel);

        PdfPCell cellValor = criarCelulaInfo(CURRENCY_FORMAT.format(bordero.getValorLiquido()), fontDestaque, true);
        cellValor.setBackgroundColor(new Color(51, 122, 183));
        resumoTable.addCell(cellValor);

        document.add(resumoTable);

        // Fórmulas de cálculo
        document.add(new Paragraph(" "));
        Paragraph tituloFormulas = new Paragraph("FÓRMULAS DE CÁLCULO", fontSubtitulo);
        tituloFormulas.setSpacingBefore(10);
        tituloFormulas.setSpacingAfter(5);
        document.add(tituloFormulas);

        Font fontFormula = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Paragraph formulas = new Paragraph();
        formulas.add(new Chunk("• Deságio = VLR_BRUTO × ((FATOR ÷ DIAS_VCTO) × (DIAS_VCTO + D+))\n", fontFormula));
        formulas.add(new Chunk("• D+ (Float) = 2 dias + ajuste para próximo dia útil (fins de semana e feriados)\n", fontFormula));
        formulas.add(new Chunk("• VLR_LÍQUIDO = VLR_BRUTO - DESÁGIO - TARIFA_TÍTULO\n", fontFormula));
        formulas.add(new Chunk("• FATOR aplicado: 1,75% a.m. (0,0175)\n", fontFormula));
        formulas.add(new Chunk("• Tarifa por título: R$ 15,00\n", fontFormula));
        formulas.add(new Chunk("• Taxa Serasa: R$ 50,00 por sacado único", fontFormula));

        document.add(formulas);
    }

    // Métodos auxiliares
    private PdfPCell criarCelulaCabecalho(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setBackgroundColor(new Color(51, 122, 183));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderColor(Color.DARK_GRAY);
        return cell;
    }

    private PdfPCell criarCelulaDado(String texto, Font font, boolean alinharDireita) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setPadding(4);
        cell.setBorderColor(Color.GRAY);

        if (alinharDireita) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        } else {
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        }

        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell criarCelulaInfo(String texto, Font font, boolean negrito) {
        if (negrito) {
            font = FontFactory.getFont(font.getFamilyname(), font.getSize(), Font.BOLD);
        }

        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private String formatarCNPJ(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." +
                cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" +
                cnpj.substring(12, 14);
    }
}