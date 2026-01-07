package com.bordero.nfe.controller;

import com.bordero.nfe.domain.dto.*;
import com.bordero.nfe.service.NFeProcessorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/nfe")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permitir CORS para interface web
public class NFeController {

    private final NFeProcessorService service;

    /**
     * Endpoint ORIGINAL - Upload via JSON (mantido para compatibilidade)
     */
    @PostMapping("/upload")
    public ResponseEntity<ProcessamentoResponse> uploadNFe(
            @Valid @RequestBody UploadNFeRequest request) {

        ProcessamentoResponse response = service.processarNFe(request);

        if (response.isSucesso()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * NOVO - Upload de arquivo XML único
     */
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessamentoResponse> uploadNFeFile(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("cnpjCliente") String cnpjCliente) {

        try {
            // Validar arquivo
            if (arquivo.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ProcessamentoResponse.builder()
                                .sucesso(false)
                                .mensagem("Arquivo vazio")
                                .erros(List.of("O arquivo XML não pode estar vazio"))
                                .build());
            }

            // Validar extensão
            String nomeArquivo = arquivo.getOriginalFilename();
            if (nomeArquivo == null || !nomeArquivo.toLowerCase().endsWith(".xml")) {
                return ResponseEntity.badRequest()
                        .body(ProcessamentoResponse.builder()
                                .sucesso(false)
                                .mensagem("Formato inválido")
                                .erros(List.of("Apenas arquivos XML são permitidos"))
                                .build());
            }

            // Converter para String
            String xmlContent = new String(arquivo.getBytes(), StandardCharsets.UTF_8);

            // Criar request
            UploadNFeRequest request = new UploadNFeRequest(xmlContent, cnpjCliente);

            // Processar
            ProcessamentoResponse response = service.processarNFe(request);

            if (response.isSucesso()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ProcessamentoResponse.builder()
                            .sucesso(false)
                            .mensagem("Erro ao processar arquivo")
                            .erros(List.of("Erro: " + e.getMessage()))
                            .build());
        }
    }

    /**
     * NOVO - Upload em lote de múltiplos arquivos XML
     */
    @PostMapping(value = "/upload-lote", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessamentoBatchResponse> uploadLoteNFe(
            @RequestParam("arquivos") List<MultipartFile> arquivos,
            @RequestParam("cnpjCliente") String cnpjCliente) {

        ProcessamentoBatchResponse batchResponse = ProcessamentoBatchResponse.builder()
                .totalArquivos(arquivos.size())
                .resultados(new ArrayList<>())
                .build();

        for (MultipartFile arquivo : arquivos) {
            ResultadoProcessamento resultado = ResultadoProcessamento.builder()
                    .nomeArquivo(arquivo.getOriginalFilename())
                    .build();

            try {
                // Validações básicas
                if (arquivo.isEmpty()) {
                    resultado.setSucesso(false);
                    resultado.setMensagem("Arquivo vazio");
                    batchResponse.incrementarErros();
                    batchResponse.addResultado(resultado);
                    continue;
                }

                String nomeArquivo = arquivo.getOriginalFilename();
                if (nomeArquivo == null || !nomeArquivo.toLowerCase().endsWith(".xml")) {
                    resultado.setSucesso(false);
                    resultado.setMensagem("Formato inválido - apenas XML permitido");
                    batchResponse.incrementarErros();
                    batchResponse.addResultado(resultado);
                    continue;
                }

                // Processar XML
                String xmlContent = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
                UploadNFeRequest request = new UploadNFeRequest(xmlContent, cnpjCliente);
                ProcessamentoResponse response = service.processarNFe(request);

                if (response.isSucesso()) {
                    // Sucesso
                    NotaFiscalDTO nfe = response.getNotaFiscal();
                    resultado.setSucesso(true);
                    resultado.setMensagem("NFe processada com sucesso");
                    resultado.setChaveAcesso(nfe.getChaveAcesso());
                    resultado.setNfeId(nfe.getId());
                    resultado.setNumeroNota(nfe.getNumeroNfe());
                    resultado.setValorTotal(nfe.getValorTotal());
                    resultado.setQuantidadeDuplicatas(nfe.getDuplicatas().size());
                    batchResponse.incrementarSucessos();
                } else {
                    // Erro no processamento
                    resultado.setSucesso(false);
                    resultado.setMensagem(response.getMensagem());
                    resultado.setErros(response.getErros());
                    batchResponse.incrementarErros();
                }

            } catch (Exception e) {
                resultado.setSucesso(false);
                resultado.setMensagem("Erro ao processar: " + e.getMessage());
                resultado.setErros(List.of(e.getMessage()));
                batchResponse.incrementarErros();
            }

            batchResponse.addResultado(resultado);
        }

        // Gerar resumo
        batchResponse.gerarResumo();

        return ResponseEntity.ok(batchResponse);
    }

    /**
     * Endpoint usado pelo Feign Client do bordero-generator-service
     */
    @GetMapping("/{id}/data")
    public ResponseEntity<NotaFiscalDTO> buscarDadosNFe(@PathVariable Long id) {
        NotaFiscalDTO nfe = service.buscarNFePorId(id);
        return ResponseEntity.ok(nfe);
    }

    /**
     * Buscar NFe por ID (endpoint genérico)
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotaFiscalDTO> buscarNFe(@PathVariable Long id) {
        NotaFiscalDTO nfe = service.buscarNFePorId(id);
        return ResponseEntity.ok(nfe);
    }

    /**
     * Listar todas as NFes com paginação (opcional)
     */
    @GetMapping
    public ResponseEntity<List<NotaFiscalDTO>> listarNFes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // TODO: Implementar paginação no service
        return ResponseEntity.ok(List.of());
    }

    /**
     * Health check do serviço
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("NFe Processor Service está UP");
    }
}
