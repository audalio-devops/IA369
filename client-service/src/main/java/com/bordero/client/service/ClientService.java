package com.bordero.client.service;

import com.bordero.client.domain.dto.*;
import com.bordero.client.domain.model.*;
import com.bordero.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository repository;
    private final DocumentValidationService documentValidationService;

    @Transactional
    public ClientDTO cadastrarCliente(ClientRegistrationRequest request) {
        // Validar duplicidade
        if (repository.existsByCnpj(request.getCnpj())) {
            throw new IllegalArgumentException("CNPJ já cadastrado");
        }

        if (repository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        // Criar cliente
        Client client = Client.builder()
                .cnpj(request.getCnpj())
                .razaoSocial(request.getRazaoSocial())
                .nomeFantasia(request.getNomeFantasia())
                .email(request.getEmail())
                .telefone(request.getTelefone())
                .inscricaoEstadual(request.getInscricaoEstadual())
                .endereco(converterEndereco(request.getEndereco()))
                .status(ClientStatus.PENDENTE)
                .limiteCredito(BigDecimal.ZERO)
                .limiteDisponivel(BigDecimal.ZERO)
                .ativo(true)
                .build();

        Client clienteSalvo = repository.save(client);

        log.info("Cliente cadastrado: {} - {}", clienteSalvo.getCnpj(),
                clienteSalvo.getRazaoSocial());

        return converterParaDTO(clienteSalvo);
    }

    public ClientDTO buscarPorId(Long id) {
        Client client = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        return converterParaDTO(client);
    }

    public ClientDTO buscarPorCnpj(String cnpj) {
        Client client = repository.findByCnpj(cnpj)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        return converterParaDTO(client);
    }

    public List<ClientDTO> listarAtivos() {
        return repository.findByAtivoTrue().stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public void atualizarLimiteCredito(Long clientId, BigDecimal novoLimite) {
        Client client = repository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        client.setLimiteCredito(novoLimite);
        client.setLimiteDisponivel(novoLimite);
        repository.save(client);

        log.info("Limite atualizado para cliente {}: {}", clientId, novoLimite);
    }

    @Transactional
    public void descontarLimite(String cnpj, BigDecimal valor) {
        Client client = repository.findByCnpj(cnpj)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        if (client.getLimiteDisponivel().compareTo(valor) < 0) {
            throw new IllegalArgumentException("Limite insuficiente");
        }

        client.setLimiteDisponivel(
                client.getLimiteDisponivel().subtract(valor)
        );
        repository.save(client);
    }

    private ClientAddress converterEndereco(AddressDTO dto) {
        return new ClientAddress(
                dto.getLogradouro(),
                dto.getNumero(),
                dto.getComplemento(),
                dto.getBairro(),
                dto.getCidade(),
                dto.getUf(),
                dto.getCep()
        );
    }

    private ClientDTO converterParaDTO(Client client) {
        return ClientDTO.builder()
                .id(client.getId())
                .cnpj(client.getCnpj())
                .razaoSocial(client.getRazaoSocial())
                .nomeFantasia(client.getNomeFantasia())
                .email(client.getEmail())
                .telefone(client.getTelefone())
                .status(client.getStatus().name())
                .limiteCredito(client.getLimiteCredito())
                .limiteDisponivel(client.getLimiteDisponivel())
                .dataCadastro(client.getDataCadastro())
                .build();
    }
}