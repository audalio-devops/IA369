package com.bordero.bordero.controller;

import com.bordero.bordero.domain.model.Bordero;
import com.bordero.bordero.service.BorderoGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bordero")
@RequiredArgsConstructor
public class BorderoController {

    private final BorderoGeneratorService service;

    @PostMapping("/gerar/{nfeId}")
    public ResponseEntity<Bordero> gerarBordero(@PathVariable Long nfeId) {
        Bordero bordero = service.gerarBorderoAutomatico(nfeId);
        return ResponseEntity.ok(bordero);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPDF(@PathVariable Long id) {
        byte[] pdf = service.gerarPDFBordero(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bordero.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
