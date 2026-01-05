package com.bordero.bordero.service;

import com.bordero.bordero.domain.model.Tarifa;
import com.bordero.bordero.domain.model.TipoTarifa;
import com.bordero.bordero.repository.TarifaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do TarifaService")
class TarifaServiceTest {

    @Mock
    private TarifaRepository tarifaRepository;

    @InjectMocks
    private TarifaService tarifaService;

    private List<Tarifa> tarifasDocumento;
    private List<Tarifa> tarifasCliente;
    private List<Tarifa> tarifasGerais;

    @BeforeEach
    void setUp() {
        // Tarifas por documento
        tarifasDocumento = List.of(
                Tarifa.builder()
                        .id(1L)
                        .tipo(TipoTarifa.DOCUMENTO)
                        .codigo("TAR_DOC")
                        .nome("Tarifa por Título")
                        .valor(new BigDecimal("2.50"))
                        .ativa(true)
                        .build()
        );

        // Tarifas por cliente
        tarifasCliente = List.of(
                Tarifa.builder()
                        .id(2L)
                        .tipo(TipoTarifa.CLIENTE)
                        .codigo("SERASA")
                        .nome("Consulta Serasa")
                        .valor(new BigDecimal("10.00"))
                        .ativa(true)
                        .build()
        );

        // Tarifas gerais
        tarifasGerais = List.of(
                Tarifa.builder()
                        .id(3L)
                        .tipo(TipoTarifa.GERAL)
                        .codigo("TAC")
                        .nome("TAC")
                        .valor(new BigDecimal("100.00"))
                        .ativa(true)
                        .build(),
                Tarifa.builder()
                        .id(4L)
                        .tipo(TipoTarifa.GERAL)
                        .codigo("TED")
                        .nome("TED")
                        .valor(new BigDecimal("50.00"))
                        .ativa(true)
                        .build()
        );

        when(tarifaRepository.findByTipoAndAtivaTrue(TipoTarifa.DOCUMENTO))
                .thenReturn(tarifasDocumento);
        when(tarifaRepository.findByTipoAndAtivaTrue(TipoTarifa.CLIENTE))
                .thenReturn(tarifasCliente);
        when(tarifaRepository.findByTipoAndAtivaTrue(TipoTarifa.GERAL))
                .thenReturn(tarifasGerais);
    }

    @Test
    @DisplayName("Deve calcular tarifas corretamente para 3 títulos com Serasa")
    void deveCalcularTarifasCorretamenteParaTresTitulosComSerasa() {
        // Arrange
        int quantidadeTitulos = 3;
        boolean incluirSerasa = true;

        // Act
        CalculoTarifasResult result = tarifaService.calcularTarifas(quantidadeTitulos, incluirSerasa);

        // Assert
        assertEquals(new BigDecimal("7.50"), result.getTarifasPorDocumento()); // 3 x 2.50
        assertEquals(new BigDecimal("10.00"), result.getTarifasPorCliente()); // Serasa
        assertEquals(new BigDecimal("150.00"), result.getTarifasGerais()); // TAC + TED
        assertEquals(new BigDecimal("167.50"), result.getValorTotal());
    }

    @Test
    @DisplayName("Deve calcular tarifas sem Serasa quando não solicitado")
    void deveCalcularTarifasSemSerasaQuandoNaoSolicitado() {
        // Arrange
        int quantidadeTitulos = 3;
        boolean incluirSerasa = false;

        // Act
        CalculoTarifasResult result = tarifaService.calcularTarifas(quantidadeTitulos, incluirSerasa);

        // Assert
        assertEquals(new BigDecimal("7.50"), result.getTarifasPorDocumento());
        assertEquals(BigDecimal.ZERO, result.getTarifasPorCliente()); // Sem Serasa
        assertEquals(new BigDecimal("150.00"), result.getTarifasGerais());
        assertEquals(new BigDecimal("157.50"), result.getValorTotal());
    }

    @Test
    @DisplayName("Deve calcular tarifas para 1 título")
    void deveCalcularTarifasParaUmTitulo() {
        // Arrange
        int quantidadeTitulos = 1;
        boolean incluirSerasa = true;

        // Act
        CalculoTarifasResult result = tarifaService.calcularTarifas(quantidadeTitulos, incluirSerasa);

        // Assert
        assertEquals(new BigDecimal("2.50"), result.getTarifasPorDocumento());
        assertEquals(new BigDecimal("10.00"), result.getTarifasPorCliente());
        assertEquals(new BigDecimal("150.00"), result.getTarifasGerais());
        assertEquals(new BigDecimal("162.50"), result.getValorTotal());
    }

    @Test
    @DisplayName("Deve calcular tarifas para 10 títulos")
    void deveCalcularTarifasParaDezTitulos() {
        // Arrange
        int quantidadeTitulos = 10;
        boolean incluirSerasa = true;

        // Act
        CalculoTarifasResult result = tarifaService.calcularTarifas(quantidadeTitulos, incluirSerasa);

        // Assert
        assertEquals(new BigDecimal("25.00"), result.getTarifasPorDocumento()); // 10 x 2.50
        assertEquals(new BigDecimal("10.00"), result.getTarifasPorCliente());
        assertEquals(new BigDecimal("150.00"), result.getTarifasGerais());
        assertEquals(new BigDecimal("185.00"), result.getValorTotal());
    }

    @Test
    @DisplayName("Deve incluir detalhamento das tarifas")
    void deveIncluirDetalhamentoDasTarifas() {
        // Arrange
        int quantidadeTitulos = 3;
        boolean incluirSerasa = true;

        // Act
        CalculoTarifasResult result = tarifaService.calcularTarifas(quantidadeTitulos, incluirSerasa);

        // Assert
        assertNotNull(result.getDetalhamento());
        assertFalse(result.getDetalhamento().isEmpty());
        assertTrue(result.getDetalhamento().containsKey("Consulta Serasa"));
        assertTrue(result.getDetalhamento().containsKey("TAC"));
        assertTrue(result.getDetalhamento().containsKey("TED"));
    }

    @Test
    @DisplayName("Deve listar apenas tarifas ativas")
    void deveListarApenasTarifasAtivas() {
        // Arrange
        List<Tarifa> todasTarifas = List.of(
                tarifasDocumento.get(0),
                tarifasCliente.get(0),
                tarifasGerais.get(0),
                tarifasGerais.get(1)
        );
        when(tarifaRepository.findByAtivaTrue()).thenReturn(todasTarifas);

        // Act
        List<Tarifa> result = tarifaService.listarAtivas();

        // Assert
        assertEquals(4, result.size());
        assertTrue(result.stream().allMatch(Tarifa::getAtiva));
    }
}