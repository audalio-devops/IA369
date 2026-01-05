package com.bordero.nfe.service;

import com.bordero.nfe.domain.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class XmlParserService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public NotaFiscal parseXml(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));

        NotaFiscal nfe = NotaFiscal.builder().build();

        // Chave de Acesso
        Element infNFe = (Element) doc.getElementsByTagName("infNFe").item(0);
        String chaveCompleta = infNFe.getAttribute("Id");
        nfe.setChaveAcesso(chaveCompleta.replace("NFe", ""));

        // Identificação
        parseIdentificacao(doc, nfe);

        // Emitente
        parseEmitente(doc, nfe);

        // Destinatário
        parseDestinatario(doc, nfe);

        // Totais
        parseTotais(doc, nfe);

        // Itens
        parseItens(doc, nfe);

        // Duplicatas
        parseDuplicatas(doc, nfe);

        // Protocolo
        parseProtocolo(doc, nfe);

        // Salvar XML original
        nfe.setXmlOriginal(xmlContent);

        log.info("XML parseado com sucesso. Chave: {}", nfe.getChaveAcesso());

        return nfe;
    }

    private void parseIdentificacao(Document doc, NotaFiscal nfe) {
        nfe.setNumeroNfe(getTextContent(doc, "nNF"));
        nfe.setSerie(getTextContent(doc, "serie"));

        String dhEmi = getTextContent(doc, "dhEmi");
        if (dhEmi != null && !dhEmi.isEmpty()) {
            nfe.setDataEmissao(LocalDateTime.parse(dhEmi, FORMATTER));
        }
    }

    private void parseEmitente(Document doc, NotaFiscal nfe) {
        NodeList emitList = doc.getElementsByTagName("emit");
        if (emitList.getLength() > 0) {
            Element emit = (Element) emitList.item(0);
            nfe.setCnpjEmitente(getElementText(emit, "CNPJ"));
            nfe.setNomeEmitente(getElementText(emit, "xNome"));
            nfe.setNomeFantasiaEmitente(getElementText(emit, "xFant"));
        }
    }

    private void parseDestinatario(Document doc, NotaFiscal nfe) {
        NodeList destList = doc.getElementsByTagName("dest");
        if (destList.getLength() > 0) {
            Element dest = (Element) destList.item(0);
            nfe.setCnpjDestinatario(getElementText(dest, "CNPJ"));
            nfe.setNomeDestinatario(getElementText(dest, "xNome"));
            nfe.setInscricaoEstadualDestinatario(getElementText(dest, "IE"));
            nfe.setEmailDestinatario(getElementText(dest, "email"));
        }
    }

    private void parseTotais(Document doc, NotaFiscal nfe) {
        nfe.setValorTotal(getBigDecimal(doc, "vNF"));
        nfe.setValorTributos(getBigDecimal(doc, "vTotTrib"));
    }

    private void parseItens(Document doc, NotaFiscal nfe) {
        NodeList detList = doc.getElementsByTagName("det");

        for (int i = 0; i < detList.getLength(); i++) {
            Element det = (Element) detList.item(i);
            Element prod = (Element) det.getElementsByTagName("prod").item(0);

            ItemNotaFiscal item = ItemNotaFiscal.builder()
                    .numeroItem(Integer.parseInt(det.getAttribute("nItem")))
                    .codigoProduto(getElementText(prod, "cProd"))
                    .descricao(getElementText(prod, "xProd"))
                    .ncm(getElementText(prod, "NCM"))
                    .cfop(getElementText(prod, "CFOP"))
                    .unidadeComercial(getElementText(prod, "uCom"))
                    .quantidadeComercial(new BigDecimal(getElementText(prod, "qCom")))
                    .valorUnitarioComercial(new BigDecimal(getElementText(prod, "vUnCom")))
                    .valorTotal(new BigDecimal(getElementText(prod, "vProd")))
                    .build();

            nfe.addItem(item);
        }
    }

    private void parseDuplicatas(Document doc, NotaFiscal nfe) {
        NodeList dupList = doc.getElementsByTagName("dup");

        for (int i = 0; i < dupList.getLength(); i++) {
            Element dup = (Element) dupList.item(i);

            String dVenc = getElementText(dup, "dVenc");
            LocalDateTime vencimento = LocalDateTime.parse(dVenc + "T00:00:00");

            Duplicata duplicata = Duplicata.builder()
                    .numeroDuplicata(getElementText(dup, "nDup"))
                    .dataVencimento(vencimento)
                    .valor(new BigDecimal(getElementText(dup, "vDup")))
                    .status(StatusDuplicata.PENDENTE)
                    .build();

            nfe.addDuplicata(duplicata);
        }
    }

    private void parseProtocolo(Document doc, NotaFiscal nfe) {
        NodeList protList = doc.getElementsByTagName("infProt");
        if (protList.getLength() > 0) {
            Element prot = (Element) protList.item(0);
            nfe.setNumeroProtocolo(getElementText(prot, "nProt"));
            nfe.setStatusAutorizacao(getElementText(prot, "cStat"));

            String dhRecbto = getElementText(prot, "dhRecbto");
            if (dhRecbto != null && !dhRecbto.isEmpty()) {
                nfe.setDataAutorizacao(LocalDateTime.parse(dhRecbto, FORMATTER));
            }
        }
    }

    private String getTextContent(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        return list.getLength() > 0 ? list.item(0).getTextContent() : "";
    }

    private String getElementText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        return list.getLength() > 0 ? list.item(0).getTextContent() : "";
    }

    private BigDecimal getBigDecimal(Document doc, String tagName) {
        String value = getTextContent(doc, tagName);
        return value.isEmpty() ? BigDecimal.ZERO : new BigDecimal(value);
    }
}
