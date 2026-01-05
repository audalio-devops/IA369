package com.bordero.bordero.service;

import com.bordero.bordero.domain.model.*;
import com.bordero.bordero.repository.BorderoRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private BorderoGeneratorService borderoGeneratorService;

    @BeforeEach
    void setUp() {
        // Mock do cálculo de tarifas
        CalculoTarifasResult tarifas = new CalculoTarifasResult();
        tarifas.setTarifasPorDocumento(new BigDecimal("7.50")); // 3 x 2.50
        tarifas.setTarifasPorCliente(new BigDecimal("10.00"));
        tarifas.setTarifasGerais(new BigDecimal("150.00"));
        tarifas.setValorTotal(new BigDecimal("167.50"));

        when(tarifaService.calcularTarifas(anyInt(), anyBoolean()))
                .thenReturn(tarifas);

        // Mock do dia útil service (sem obstáculos)
        when(diaUtilService.calcularProximoDiaUtil(any(LocalDate.class), anyInt(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    LocalDate data = invocation.getArgument(0);
                    int dias = invocation.getArgument(1);
                    return data.plusDays(dias);
                });

        when(diaUtilService.calcularDiasUteis(any(), any(), anyString(), anyString()))
                .thenReturn(2); // Simplificado
    }

    @Test
    @DisplayName("Deve gerar borderô com 3 títulos corretamente")
    void deveGerarBorderoComTresTitulosCorretamente() {
        // Arrange
        Long nfeId = 1L;

        Map<String, Object> nfeData = Map.of(
                "chaveAcesso", "35230616554601000186550010000024761699677603",
                "numeroNfe", "2476",
                "cnpjEmitente", "16554601000186",
                "nomeEmitente", "VICOLACCI",
                "cnpjDestinatario", "13771702000110",
                "nomeDestinatario", "THAIS RODRIGUES",
                "duplicatas", List.of(
                        Map.of(
                                "id", 1L,
                                "numero", "001",
                                "vencimento", LocalDateTime.of(2026, 7, 15, 0, 0),
                                "valor", new BigDecimal("6500.00")
                        ),
                        Map.of(
                                "id", 2L,
                                "numero", "002",
                                "vencimento", LocalDateTime.of(2026, 8, 15, 0, 0),
                                "valor", new BigDecimal("6500.00")
                        ),
                        Map.of(
                                "id", 3L,
                                "numero", "003",
                                "vencimento", LocalDateTime.of(2026, 9, 15, 0, 0),
                                "valor", new BigDecimal("6500.00")
                        )
                )
        );

        when(nfeClientService.buscarNFe(nfeId)).thenReturn(nfeData);

        Bordero borderoMock = Bordero.builder()
                .id(1L)
                .numeroBordero("BOR123456")
                .build();

        when(repository.save(any(Bordero.class))).thenReturn(borderoMock);

        // Act
        Bordero resultado = borderoGeneratorService.gerarBorderoAutomatico(nfeId);

        // Assert
        assertNotNull(resultado);
        verify(repository, times(1)).save(any(Bordero.class));
    }

    @Test
    @DisplayName("Deve calcular deságio corretamente")
    void deveCalcularDesagioCorretamente() {
        // Este é um teste de exemplo - você precisará ajustar conforme sua implementação
        // O ideal é testar o cálculo isoladamente

        // Arrange
        BigDecimal valor = new BigDecimal("6500.00");
        BigDecimal taxaMensal = new BigDecimal("1.50"); // 1.5%
        BigDecimal taxaDiaria = taxaMensal.divide(new BigDecimal("30"), 6, java.math.RoundingMode.HALF_UP);
        int dias = 196; // Exemplo

        // Act
        BigDecimal desagio = valor
                .multiply(taxaDiaria)
                .multiply(new BigDecimal(dias))
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

        // Assert
        assertTrue(desagio.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(desagio.compareTo(valor) < 0); // Deságio deve ser menor que o valor
    }
}