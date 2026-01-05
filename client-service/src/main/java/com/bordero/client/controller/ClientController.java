package com.bordero.client.controller;

import com.bordero.client.domain.dto.*;
import com.bordero.client.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientDTO> cadastrar(
            @Valid @RequestBody ClientRegistrationRequest request) {
        ClientDTO client = service.cadastrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(client);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ClientDTO> buscarPorId(@PathVariable Long id) {
        ClientDTO client = service.buscarPorId(id);
        return ResponseEntity.ok(client);
    }

    @GetMapping("/cnpj/{cnpj}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ClientDTO> buscarPorCnpj(@PathVariable String cnpj) {
        ClientDTO client = service.buscarPorCnpj(cnpj);
        return ResponseEntity.ok(client);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<ClientDTO>> listarAtivos() {
        List<ClientDTO> clients = service.listarAtivos();
        return ResponseEntity.ok(clients);
    }

    @PutMapping("/{id}/limite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> atualizarLimite(
            @PathVariable Long id,
            @RequestParam BigDecimal limite) {
        service.atualizarLimiteCredito(id, limite);
        return ResponseEntity.noContent().build();
    }
}