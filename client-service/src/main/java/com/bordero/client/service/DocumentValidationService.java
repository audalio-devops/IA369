package com.bordero.client.service;

import com.bordero.client.domain.model.*;
import com.bordero.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentValidationService {

    private final ClientRepository clientRepository;

    private static final String UPLOAD_DIR = "uploads/client-documents/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "jpg", "jpeg", "png");

    /**
     * Faz upload de documento do cliente
     */
    @Transactional
    public ClientDocument uploadDocumento(Long clientId, DocumentType tipo,
                                          MultipartFile file) throws IOException {

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Validações
        validarArquivo(file);

        // Gerar nome único para o arquivo
        String nomeOriginal = file.getOriginalFilename();
        String extensao = obterExtensao(nomeOriginal);
        String nomeUnico = UUID.randomUUID().toString() + "." + extensao;

        // Criar diretório se não existir
        Path uploadPath = Paths.get(UPLOAD_DIR + clientId);
        Files.createDirectories(uploadPath);

        // Salvar arquivo
        Path filePath = uploadPath.resolve(nomeUnico);
        Files.copy(file.getInputStream(), filePath);

        // Criar registro no banco
        ClientDocument document = ClientDocument.builder()
                .client(client)
                .tipo(tipo)
                .nomeArquivo(nomeOriginal)
                .caminhoArquivo(filePath.toString())
                .tamanhoBytes(file.getSize())
                .status(DocumentStatus.PENDENTE)
                .build();

        client.getDocumentos().add(document);
        clientRepository.save(client);

        log.info("Documento uploadado: {} para cliente ID {}", tipo, clientId);

        return document;
    }

    /**
     * Valida um documento
     */
    @Transactional
    public void validarDocumento(Long documentId, boolean aprovado,
                                 String observacoes, String validadorNome) {

        Client client = clientRepository.findAll().stream()
                .filter(c -> c.getDocumentos().stream()
                        .anyMatch(d -> d.getId().equals(documentId)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        ClientDocument document = client.getDocumentos().stream()
                .filter(d -> d.getId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        document.setStatus(aprovado ? DocumentStatus.VALIDADO : DocumentStatus.REJEITADO);
        document.setDataValidacao(LocalDateTime.now());
        document.setValidadoPor(validadorNome);
        document.setObservacoes(observacoes);

        clientRepository.save(client);

        log.info("Documento {} {}: {}",
                documentId,
                aprovado ? "aprovado" : "rejeitado",
                document.getTipo());

        // Verificar se todos os documentos obrigatórios foram validados
        verificarDocumentacaoCompleta(client);
    }

    /**
     * Verifica se o cliente tem todos os documentos obrigatórios validados
     */
    private void verificarDocumentacaoCompleta(Client client) {
        List<DocumentType> obrigatorios = List.of(
                DocumentType.CONTRATO_SOCIAL,
                DocumentType.CARTAO_CNPJ,
                DocumentType.COMPROVANTE_ENDERECO
        );

        boolean todosValidados = obrigatorios.stream()
                .allMatch(tipo -> client.getDocumentos().stream()
                        .anyMatch(doc -> doc.getTipo() == tipo &&
                                doc.getStatus() == DocumentStatus.VALIDADO));

        if (todosValidados && client.getStatus() == ClientStatus.PENDENTE) {
            client.setStatus(ClientStatus.EM_ANALISE);
            clientRepository.save(client);
            log.info("Cliente {} com documentação completa, movido para EM_ANALISE",
                    client.getId());
        }
    }

    /**
     * Lista documentos de um cliente
     */
    public List<ClientDocument> listarDocumentos(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        return client.getDocumentos();
    }

    /**
     * Baixa um documento
     */
    public byte[] baixarDocumento(Long documentId) throws IOException {
        Client client = clientRepository.findAll().stream()
                .filter(c -> c.getDocumentos().stream()
                        .anyMatch(d -> d.getId().equals(documentId)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        ClientDocument document = client.getDocumentos().stream()
                .filter(d -> d.getId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        Path filePath = Paths.get(document.getCaminhoArquivo());
        return Files.readAllBytes(filePath);
    }

    // ========== Métodos de Validação ==========

    private void validarArquivo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo muito grande. Máximo: 10MB");
        }

        String nomeArquivo = file.getOriginalFilename();
        if (nomeArquivo == null || nomeArquivo.isEmpty()) {
            throw new IllegalArgumentException("Nome de arquivo inválido");
        }

        String extensao = obterExtensao(nomeArquivo).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extensao)) {
            throw new IllegalArgumentException(
                    "Extensão não permitida. Permitidas: " + ALLOWED_EXTENSIONS
            );
        }
    }

    private String obterExtensao(String nomeArquivo) {
        int lastDot = nomeArquivo.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return nomeArquivo.substring(lastDot + 1);
    }
}