package com.bordero.nfe.controller;

import com.bordero.nfe.domain.dto.*;
import com.bordero.nfe.service.NFeProcessorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/nfe")
@RequiredArgsConstructor
public class NFeController {

    private final NFeProcessorService service;

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

    @GetMapping("/{id}/data")
    public ResponseEntity<NotaFiscalDTO> buscarDadosNFe(@PathVariable Long id) {
        NotaFiscalDTO nfe = service.buscarNFePorId(id);
        return ResponseEntity.ok(nfe);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotaFiscalDTO> buscarNFe(@PathVariable Long id) {
        // Implementar busca
        return ResponseEntity.ok().build();
    }
}

