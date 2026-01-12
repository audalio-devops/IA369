package com.bordero.bordero.service;

import com.bordero.bordero.client.ClientServiceClient;
import com.bordero.bordero.domain.model.*;
import com.bordero.bordero.repository.BorderoRepository;
import com.bordero.bordero.repository.TipoTituloRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do BorderoGeneratorService")
class BorderoGeneratorServiceTest {

    @Mock
    private BorderoRepository repository;

    @Mock
    private NFeClientService nfeClientService;

    @Mock
    private TarifaService tarifaService;

    @Mock
    private DiaUtilService diaUtilService;

    @Mock
    private ClientServiceClient clientServiceClient;

    @Mock
    private TipoTituloRepository tipoTituloRepository;

    @Mock
    private PDFBorderoService pdfBorderoService;

    @InjectMocks
    private BorderoGeneratorService borderoGeneratorService;

    private TipoTitulo tipoNF;

    @BeforeEach
    void setUp() {
        // Mock do TipoTitulo
        tipoNF = TipoTitulo.builder()
                .id(1L)
                .tipo("NF")
                .nome("Nota Fiscal")
                .descricao("Título existente em uma nota fiscal")
                .ativo(true)
                .build();

        when(tipoTituloRepository.findByTipo("NF"))
                .thenReturn(Optional.of(tipoNF));

        // Mock do cálculo de tarifas
        CalculoTarifasResult tarifas = new CalculoTarifasResult();
        tarifas.setTarifasPorDocumento(new BigDecimal("45.00")); // 3 x 15.00
        tarifas.setTarifasPorCliente(new BigDecimal("50.00"));
        tarifas.setTarifasGerais(new BigDecimal("150.00"));
        tarifas.setValorTotal(new BigDecimal("245.00"));

        when(tarifaService.calcularTarifas(anyInt(), anyBoolean()))
                .thenReturn(tarifas);

        // Mock do dia útil service
        when(diaUtilService.calcularProximoDiaUtil(any(LocalDate.class), anyInt(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    LocalDate data = invocation.getArgument(0);
                    int dias = invocation.getArgument(1);
                    return data.plusDays(dias);
                });

        when(diaUtilService.calcularDiasUteis(any(), any(), anyString(), anyString()))
                .thenReturn(30);
    }

    @Test
    @DisplayName("Deve gerar borderô com 3 títulos corretamente")
    void deveGerarBorderoComTresTitulosCorretamente() {
        // Arrange
        Long nfeId = 1L;

        // Mock dos dados da NFe
        Map<String, Object> emitente = Map.of(
                "cnpj", "16554601000186",
                "razaoSocial", "VICOLACCI"
        );

        Map<String, Object> destinatario = Map.of(
                "cnpj", "13771702000110",
                "razaoSocial", "THAIS RODRIGUES"
        );

        Map<String, Object> nfeData = Map.of(
                "chaveAcesso", "35230616554601000186550010000024761699677603",
                "numeroNfe", "2476",
                "emitente", emitente,
                "destinatario", destinatario,
                "duplicatas", List.of(
                        Map.of(
                                "id", 1L,
                                "numero", "001",
                                "vencimento", "2026-07-15T00:00:00",
                                "valor", new BigDecimal("6500.00")
                        ),
                        Map.of(
                                "id", 2L,
                                "numero", "002",
                                "vencimento", "2026-08-15T00:00:00",
                                "valor", new BigDecimal("6500.00")
                        ),
                        Map.of(
                                "id", 3L,
                                "numero", "003",
                                "vencimento", "2026-09-15T00:00:00",
                                "valor", new BigDecimal("6500.00")
                        )
                )
        );

        when(nfeClientService.buscarNFe(nfeId)).thenReturn(nfeData);

        Bordero borderoMock = Bordero.builder()
                .id(1L)
                .numeroBordero("BOR123456")
                .quantidadeTitulos(3)
                .quantidadeSacados(1)
                .valorBruto(new BigDecimal("19500.00"))
                .valorLiquido(new BigDecimal("18000.00"))
                .build();

        when(repository.save(any(Bordero.class))).thenReturn(borderoMock);

        // Act
        Bordero resultado = borderoGeneratorService.gerarBorderoAutomatico(nfeId);

        // Assert
        assertNotNull(resultado);
        assertEquals("BOR123456", resultado.getNumeroBordero());
        verify(repository, times(1)).save(any(Bordero.class));
        verify(nfeClientService, times(1)).buscarNFe(nfeId);
    }

    @Test
    @DisplayName("Deve calcular deságio corretamente conforme fórmula")
    void deveCalcularDesagioCorretamente() {
        // Teste unitário do cálculo de deságio
        // Fórmula: Deságio = VLR_BRUTO × ((FATOR ÷ DIAS_VCTO) × (DIAS_VCTO + D+))

        // Arrange
        BigDecimal valorBruto = new BigDecimal("6500.00");
        BigDecimal fator = new BigDecimal("0.0175"); // 1.75%
        int diasVcto = 30;
        int floatDias = 2; // D+

        // Act
        // (FATOR / DIAS_VCTO) * (DIAS_VCTO + D+)
        BigDecimal diasVctoBD = new BigDecimal(diasVcto);
        BigDecimal diasFloatBD = new BigDecimal(floatDias);
        BigDecimal diasTotal = diasVctoBD.add(diasFloatBD);

        BigDecimal taxaDiaria = fator.divide(diasVctoBD, 10, java.math.RoundingMode.HALF_UP);
        BigDecimal desagio = valorBruto
                .multiply(taxaDiaria)
                .multiply(diasTotal)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        // Assert
        assertTrue(desagio.compareTo(BigDecimal.ZERO) > 0, "Deságio deve ser positivo");
        assertTrue(desagio.compareTo(valorBruto) < 0, "Deságio deve ser menor que valor bruto");

        // Cálculo esperado:
        // taxaDiaria = 0.0175 / 30 = 0.000583333...
        // desagio = 6500 * 0.000583333 * 32 = 121.33
        BigDecimal esperado = new BigDecimal("121.33");
        assertEquals(esperado, desagio, "Deságio deve ser calculado corretamente");
    }

    @Test
    @DisplayName("Deve gerar borderô com múltiplas NFes")
    void deveGerarBorderoComMultiplasNFes() {
        // Arrange
        String cnpjCliente = "12345678000190";
        List<Long> nfeIds = List.of(1L, 2L);

        Map<String, Object> emitente = Map.of(
                "cnpj", "16554601000186",
                "razaoSocial", "VICOLACCI"
        );

        Map<String, Object> destinatario = Map.of(
                "cnpj", "13771702000110",
                "razaoSocial", "THAIS RODRIGUES"
        );

        Map<String, Object> nfeData1 = Map.of(
                "chaveAcesso", "35230616554601000186550010000024761699677603",
                "numeroNfe", "2476",
                "emitente", emitente,
                "destinatario", destinatario,
                "duplicatas", List.of(
                        Map.of("id", 1L, "numero", "001", "vencimento", "2026-07-15T00:00:00", "valor", new BigDecimal("1000.00"))
                )
        );

        Map<String, Object> nfeData2 = Map.of(
                "chaveAcesso", "35230616554601000186550010000024771699677604",
                "numeroNfe", "2477",
                "emitente", emitente,
                "destinatario", destinatario,
                "duplicatas", List.of(
                        Map.of("id", 2L, "numero", "001", "vencimento", "2026-08-15T00:00:00", "valor", new BigDecimal("2000.00"))
                )
        );

        when(nfeClientService.buscarNFe(1L)).thenReturn(nfeData1);
        when(nfeClientService.buscarNFe(2L)).thenReturn(nfeData2);

        Bordero borderoMock = Bordero.builder()
                .id(1L)
                .numeroBordero("BOR123457")
                .cnpjCliente(cnpjCliente)
                .quantidadeTitulos(2)
                .quantidadeSacados(1)
                .build();

        when(repository.save(any(Bordero.class))).thenReturn(borderoMock);

        // Act
        Bordero resultado = borderoGeneratorService.gerarBorderoComMultiplasNFes(cnpjCliente, nfeIds);

        // Assert
        assertNotNull(resultado);
        assertEquals(cnpjCliente, resultado.getCnpjCliente());
        assertEquals(2, resultado.getQuantidadeTitulos());
        verify(nfeClientService, times(2)).buscarNFe(anyLong());
        verify(repository, times(1)).save(any(Bordero.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando lista de NFes estiver vazia")
    void deveLancarExcecaoQuandoListaNFesVazia() {
        // Arrange
        String cnpjCliente = "12345678000190";
        List<Long> nfeIdsVazio = List.of();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            borderoGeneratorService.gerarBorderoComMultiplasNFes(cnpjCliente, nfeIdsVazio);
        });
    }

    @Test
    @DisplayName("Deve contar sacados únicos corretamente")
    void deveContarSacadosUnicosCorretamente() {
        // Arrange
        String cnpjCliente = "12345678000190";
        List<Long> nfeIds = List.of(1L, 2L);

        Map<String, Object> emitente = Map.of(
                "cnpj", "16554601000186",
                "razaoSocial", "VICOLACCI"
        );

        // Primeiro destinatário
        Map<String, Object> destinatario1 = Map.of(
                "cnpj", "11111111000111",
                "razaoSocial", "CLIENTE A"
        );

        // Segundo destinatário (diferente)
        Map<String, Object> destinatario2 = Map.of(
                "cnpj", "22222222000122",
                "razaoSocial", "CLIENTE B"
        );

        Map<String, Object> nfeData1 = Map.of(
                "chaveAcesso", "35230616554601000186550010000024761699677603",
                "numeroNfe", "2476",
                "emitente", emitente,
                "destinatario", destinatario1,
                "duplicatas", List.of(
                        Map.of("id", 1L, "numero", "001", "vencimento", "2026-07-15T00:00:00", "valor", new BigDecimal("1000.00"))
                )
        );

        Map<String, Object> nfeData2 = Map.of(
                "chaveAcesso", "35230616554601000186550010000024771699677604",
                "numeroNfe", "2477",
                "emitente", emitente,
                "destinatario", destinatario2,
                "duplicatas", List.of(
                        Map.of("id", 2L, "numero", "001", "vencimento", "2026-08-15T00:00:00", "valor", new BigDecimal("2000.00"))
                )
        );

        when(nfeClientService.buscarNFe(1L)).thenReturn(nfeData1);
        when(nfeClientService.buscarNFe(2L)).thenReturn(nfeData2);

        Bordero borderoMock = Bordero.builder()
                .id(1L)
                .numeroBordero("BOR123458")
                .quantidadeSacados(2) // 2 sacados diferentes
                .build();

        when(repository.save(any(Bordero.class))).thenReturn(borderoMock);

        // Act
        Bordero resultado = borderoGeneratorService.gerarBorderoComMultiplasNFes(cnpjCliente, nfeIds);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.getQuantidadeSacados(), "Deve contar 2 sacados únicos");
    }
}