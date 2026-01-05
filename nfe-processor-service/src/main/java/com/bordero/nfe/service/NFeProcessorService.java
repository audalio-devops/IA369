package com.bordero.nfe.service;

import com.bordero.nfe.domain.dto.*;
import com.bordero.nfe.domain.model.*;
import com.bordero.nfe.repository.NotaFiscalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class NFeProcessorService {

    private final XmlParserService xmlParserService;
    private final NotaFiscalRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public ProcessamentoResponse processarNFe(UploadNFeRequest request) {
        List<String> erros = new ArrayList<>();

        try {
            // 1. Validar XML
            validarXml(request.getXmlContent());

            // 2. Parser XML
            NotaFiscal nfe = xmlParserService.parseXml(request.getXmlContent());

            // 3. Validar regras de negócio
            validarRegrasNegocio(nfe, erros);

            if (!erros.isEmpty()) {
                return ProcessamentoResponse.builder()
                        .sucesso(false)
                        .mensagem("Erros de validação encontrados")
                        .erros(erros)
                        .build();
            }

            // 4. Verificar duplicidade
            if (repository.existsByChaveAcesso(nfe.getChaveAcesso())) {
                erros.add("NF-e já foi processada anteriormente");
                return ProcessamentoResponse.builder()
                        .sucesso(false)
                        .mensagem("NF-e duplicada")
                        .erros(erros)
                        .build();
            }

            // 5. Salvar NF-e
            nfe.setStatus(StatusProcessamento.PROCESSADO);
            NotaFiscal nfeSalva = repository.save(nfe);

            // 6. Publicar evento para gerar borderô
            publicarEventoNFeProcessada(nfeSalva);

            // 7. Converter para DTO
            NotaFiscalDTO dto = converterParaDTO(nfeSalva);

            log.info("NF-e processada com sucesso: {}", nfe.getChaveAcesso());

            return ProcessamentoResponse.builder()
                    .sucesso(true)
                    .mensagem("NF-e processada com sucesso")
                    .notaFiscal(dto)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao processar NF-e", e);
            erros.add("Erro ao processar XML: " + e.getMessage());
            return ProcessamentoResponse.builder()
                    .sucesso(false)
                    .mensagem("Erro no processamento")
                    .erros(erros)
                    .build();
        }
    }

    // Método novo para ser usado pelo Controller
    public NotaFiscalDTO buscarNFePorId(Long id) {
        NotaFiscal nfe = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota Fiscal não encontrada para o ID: " + id));

        return converterParaDTO(nfe);
    }

    private void validarXml(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new IllegalArgumentException("XML não pode estar vazio");
        }
        if (!xml.contains("<NFe") && !xml.contains("<nfeProc")) {
            throw new IllegalArgumentException("XML inválido - não é uma NF-e");
        }
    }

    private void validarRegrasNegocio(NotaFiscal nfe, List<String> erros) {
        // Validar status de autorização
        if (!"100".equals(nfe.getStatusAutorizacao())) {
            erros.add("NF-e não está autorizada (Status: " + nfe.getStatusAutorizacao() + ")");
        }

        // Validar duplicatas
        if (nfe.getDuplicatas().isEmpty()) {
            erros.add("NF-e não possui duplicatas para antecipação");
        }

        // Validar destinatário
        if (nfe.getCnpjDestinatario() == null || nfe.getCnpjDestinatario().length() != 14) {
            erros.add("CNPJ do destinatário inválido");
        }
    }

    private void publicarEventoNFeProcessada(NotaFiscal nfe) {
        Map<String, Object> evento = Map.of(
                "tipo", "NFE_PROCESSADA",
                "nfeId", nfe.getId(),
                "chaveAcesso", nfe.getChaveAcesso(),
                "cnpjEmitente", nfe.getCnpjEmitente(),
                "valorTotal", nfe.getValorTotal(),
                "quantidadeDuplicatas", nfe.getDuplicatas().size()
        );

        kafkaTemplate.send("nfe-events", evento);
        log.info("Evento publicado: NFE_PROCESSADA - {}", nfe.getChaveAcesso());
    }

    private NotaFiscalDTO converterParaDTO(NotaFiscal nfe) {
        return NotaFiscalDTO.builder()
                .id(nfe.getId())
                .chaveAcesso(nfe.getChaveAcesso())
                .numeroNfe(nfe.getNumeroNfe())
                .serie(nfe.getSerie())
                .dataEmissao(nfe.getDataEmissao())
                .valorTotal(nfe.getValorTotal())
                .valorTributos(nfe.getValorTributos())
                .emitente(EmitenteDTO.builder()
                        .cnpj(nfe.getCnpjEmitente())
                        .razaoSocial(nfe.getNomeEmitente())
                        .nomeFantasia(nfe.getNomeFantasiaEmitente())
                        .build())
                .destinatario(DestinatarioDTO.builder()
                        .cnpj(nfe.getCnpjDestinatario())
                        .razaoSocial(nfe.getNomeDestinatario())
                        .inscricaoEstadual(nfe.getInscricaoEstadualDestinatario())
                        .email(nfe.getEmailDestinatario())
                        .build())
                .duplicatas(nfe.getDuplicatas().stream()
                        .map(d -> DuplicataDTO.builder()
                                .numero(d.getNumeroDuplicata())
                                .vencimento(d.getDataVencimento())
                                .valor(d.getValor())
                                .status(d.getStatus().name())
                                .build())
                        .toList())
                .status(nfe.getStatus().name())
                .build();
    }
}
