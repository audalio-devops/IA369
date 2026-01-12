package com.bordero.bordero.controller;

import com.bordero.bordero.domain.model.Bordero;
import com.bordero.bordero.dto.BorderoDTO;
import com.bordero.bordero.dto.GerarBorderoRequest;
import com.bordero.bordero.mapper.BorderoMapper;
import com.bordero.bordero.service.BorderoGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller para gerenciamento de Borderôs
 * VERSÃO COM VALIDAÇÕES ROBUSTAS
 */
@RestController
@RequestMapping("/api/borderos")
@RequiredArgsConstructor
@Slf4j
@Validated
public class BorderoController {

    private final BorderoGeneratorService borderoService;
    private final BorderoMapper borderoMapper;

    /**
     * POST /api/borderos/gerar
     * Gera um borderô com múltiplas NFes
     *
     * Body exemplo:
     * {
     *   "cnpjCliente": "12345678000190",
     *   "nfeIds": [1, 2, 3]
     * }
     */
    @PostMapping("/gerar")
    public ResponseEntity<?> gerarBordero(@Valid @RequestBody GerarBorderoRequest request) {
        try {
            log.info("Recebida solicitação para gerar borderô: cliente={}, nfeIds={}",
                    request.getCnpjCliente(), request.getNfeIds());

            Bordero bordero = borderoService.gerarBorderoComMultiplasNFes(
                    request.getCnpjCliente(),
                    request.getNfeIds()
            );

            BorderoDTO dto = borderoMapper.toDTO(bordero);

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (IllegalArgumentException e) {
            log.error("Erro de validação ao gerar borderô: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(criarErroResponse("Erro de validação", e.getMessage()));

        } catch (RuntimeException e) {
            log.error("Erro de negócio ao gerar borderô", e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(criarErroResponse("Erro ao processar borderô", e.getMessage()));

        } catch (Exception e) {
            log.error("Erro inesperado ao gerar borderô", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(criarErroResponse("Erro interno", "Ocorreu um erro ao processar sua solicitação"));
        }
    }

    /**
     * GET /api/borderos/{id}
     * Busca borderô por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            log.info("Buscando borderô: id={}", id);

            Bordero bordero = borderoService.buscarPorId(id);
            BorderoDTO dto = borderoMapper.toDTO(bordero);

            return ResponseEntity.ok(dto);

        } catch (RuntimeException e) {
            log.error("Borderô não encontrado: id={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(criarErroResponse("Borderô não encontrado",
                            "Não foi encontrado borderô com ID: " + id));
        } catch (Exception e) {
            log.error("Erro ao buscar borderô", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(criarErroResponse("Erro interno", "Erro ao buscar borderô"));
        }
    }

    /**
     * GET /api/borderos
     * Lista borderôs (opcionalmente filtrado por status)
     * Query params: ?status=GERADO
     */
    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String status) {
        try {
            log.info("Listando borderôs: status={}", status);

            List<Bordero> borderos = borderoService.listarPorStatus(status);
            List<BorderoDTO> dtos = borderoMapper.toDTOList(borderos);

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Erro ao listar borderôs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(criarErroResponse("Erro interno", "Erro ao listar borderôs"));
        }
    }

    /**
     * GET /api/borderos/{id}/pdf
     * Gera e retorna o PDF do borderô
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> gerarPDF(@PathVariable Long id) {
        try {
            log.info("Gerando PDF do borderô: id={}", id);

            byte[] pdfBytes = borderoService.gerarPDFBordero(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "bordero_" + id + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (RuntimeException e) {
            log.error("Erro ao gerar PDF do borderô: id={}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(criarErroResponse("Borderô não encontrado",
                            "Não foi possível gerar PDF para o borderô ID: " + id));

        } catch (Exception e) {
            log.error("Erro ao gerar PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(criarErroResponse("Erro interno", "Erro ao gerar PDF"));
        }
    }

    /**
     * GET /api/borderos/{id}/titulos
     * Busca apenas os títulos de um borderô específico
     */
    @GetMapping("/{id}/titulos")
    public ResponseEntity<?> buscarTitulosDoBordero(@PathVariable Long id) {
        try {
            log.info("Buscando títulos do borderô: id={}", id);

            Bordero bordero = borderoService.buscarPorId(id);

            // Retorna apenas os títulos (sem o borderô completo)
            List<com.bordero.bordero.dto.TituloBorderoDTO> titulos =
                    borderoMapper.toTituloDTOList(bordero.getTitulos());

            return ResponseEntity.ok(titulos);

        } catch (RuntimeException e) {
            log.error("Borderô não encontrado: id={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(criarErroResponse("Borderô não encontrado",
                            "Não foi encontrado borderô com ID: " + id));
        }
    }

    /**
     * DELETE /api/borderos/{id}
     * Exclui um borderô (lógica de exclusão pode ser implementada)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        try {
            log.warn("Tentativa de exclusão de borderô: id={}", id);

            // TODO: Implementar lógica de exclusão/cancelamento
            // borderoService.excluir(id);

            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(criarErroResponse("Funcionalidade não implementada",
                            "Exclusão de borderôs ainda não está disponível"));

        } catch (Exception e) {
            log.error("Erro ao excluir borderô", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(criarErroResponse("Erro interno", "Erro ao excluir borderô"));
        }
    }

    /**
     * Método auxiliar para criar respostas de erro padronizadas
     */
    private Map<String, Object> criarErroResponse(String erro, String mensagem) {
        Map<String, Object> response = new HashMap<>();
        response.put("erro", erro);
        response.put("mensagem", mensagem);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Exception handler para validações do Bean Validation
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        Map<String, Object> errors = new HashMap<>();
        errors.put("erro", "Erro de validação");
        errors.put("timestamp", System.currentTimeMillis());

        List<String> mensagens = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        errors.put("mensagens", mensagens);

        return ResponseEntity.badRequest().body(errors);
    }
}