package com.bordero.bordero.controller;

import com.bordero.bordero.domain.model.Bordero;
import com.bordero.bordero.domain.model.StatusBordero;
import com.bordero.bordero.dto.BorderoDTO;
import com.bordero.bordero.mapper.BorderoMapper;
import com.bordero.bordero.service.BorderoGeneratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes do BorderoController
 */
@WebMvcTest(BorderoController.class)
class BorderoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BorderoGeneratorService borderoService;

    @MockBean
    private BorderoMapper borderoMapper;

    private Bordero borderoMock;
    private BorderoDTO borderoDTO;

    @BeforeEach
    void setUp() {
        // Criar mock de Bordero
        borderoMock = Bordero.builder()
                .id(1L)
                .numeroBordero("BOR123456")
                .dataGeracao(LocalDateTime.now())
                .cnpjCedente("12345678000190")
                .nomeCedente("Empresa Teste LTDA")
                .cnpjCliente("98765432000110")
                .valorBruto(new BigDecimal("10000.00"))
                .valorDesagio(new BigDecimal("175.00"))
                .valorLiquido(new BigDecimal("9825.00"))
                .quantidadeTitulos(5)
                .quantidadeSacados(3)
                .status(StatusBordero.GERADO)
                .build();

        // Criar mock de BorderoDTO
        borderoDTO = BorderoDTO.builder()
                .id(1L)
                .numeroBordero("BOR123456")
                .dataGeracao(borderoMock.getDataGeracao())
                .cnpjCedente("12345678000190")
                .nomeCedente("Empresa Teste LTDA")
                .cnpjCliente("98765432000110")
                .valorBruto(new BigDecimal("10000.00"))
                .valorDesagio(new BigDecimal("175.00"))
                .valorLiquido(new BigDecimal("9825.00"))
                .quantidadeTitulos(5)
                .quantidadeSacados(3)
                .status(StatusBordero.GERADO)
                .build();
    }

    @Test
    void deveGerarBorderoComSucesso() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("cnpjCliente", "98765432000110");
        request.put("nfeIds", List.of(1L, 2L, 3L));

        when(borderoService.gerarBorderoComMultiplasNFes(anyString(), anyList()))
                .thenReturn(borderoMock);
        when(borderoMapper.toDTO(any(Bordero.class)))
                .thenReturn(borderoDTO);

        // Act & Assert
        mockMvc.perform(post("/api/borderos/gerar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numeroBordero").value("BOR123456"))
                .andExpect(jsonPath("$.cnpjCliente").value("98765432000110"))
                .andExpect(jsonPath("$.quantidadeTitulos").value(5))
                .andExpect(jsonPath("$.valorBruto").value(10000.00))
                .andExpect(jsonPath("$.valorLiquido").value(9825.00));
    }

    @Test
    void deveBuscarBorderoPorId() throws Exception {
        // Arrange
        when(borderoService.buscarPorId(1L)).thenReturn(borderoMock);
        when(borderoMapper.toDTO(any(Bordero.class))).thenReturn(borderoDTO);

        // Act & Assert
        mockMvc.perform(get("/api/borderos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numeroBordero").value("BOR123456"));
    }

    @Test
    void deveRetornar404QuandoBorderoNaoEncontrado() throws Exception {
        // Arrange
        when(borderoService.buscarPorId(999L))
                .thenThrow(new RuntimeException("Borderô não encontrado"));

        // Act & Assert
        mockMvc.perform(get("/api/borderos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarBorderos() throws Exception {
        // Arrange
        List<Bordero> borderos = List.of(borderoMock);
        List<BorderoDTO> borderosDTO = List.of(borderoDTO);

        when(borderoService.listarPorStatus(null)).thenReturn(borderos);
        when(borderoMapper.toDTOList(anyList())).thenReturn(borderosDTO);

        // Act & Assert
        mockMvc.perform(get("/api/borderos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].numeroBordero").value("BOR123456"));
    }

    @Test
    void deveGerarPDFDoBordero() throws Exception {
        // Arrange
        byte[] pdfBytes = "PDF_MOCK_CONTENT".getBytes();
        when(borderoService.gerarPDFBordero(1L)).thenReturn(pdfBytes);

        // Act & Assert
        mockMvc.perform(get("/api/borderos/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "form-data; name=\"filename\"; filename=\"bordero_1.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void deveRetornarBadRequestQuandoListaNFeVazia() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("cnpjCliente", "98765432000110");
        request.put("nfeIds", List.of());

        when(borderoService.gerarBorderoComMultiplasNFes(anyString(), anyList()))
                .thenThrow(new IllegalArgumentException("Lista de NFes não pode ser vazia"));

        // Act & Assert
        mockMvc.perform(post("/api/borderos/gerar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}