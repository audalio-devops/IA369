package com.bordero.bordero.integration;

import com.bordero.bordero.domain.model.*;
import com.bordero.bordero.repository.*;
import com.bordero.bordero.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Testes de Integração do Borderô")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BorderoIntegrationTest {

    @Autowired
    private TarifaRepository tarifaRepository;

    @Autowired
    private FeriadoRepository feriadoRepository;

    @Autowired
    private TarifaService tarifaService;

    @Autowired
    private DiaUtilService diaUtilService;

    @BeforeEach
    void setUp() {
        // Limpar dados
        tarifaRepository.deleteAll();
        feriadoRepository.deleteAll();

        // Inserir tarifas
        tarifaRepository.save(Tarifa.builder()
                .tipo(TipoTarifa.DOCUMENTO)
                .codigo("TAR_DOC")
                .nome("Tarifa por Título")
                .valor(new BigDecimal("2.50"))
                .ativa(true)
                .build());

        tarifaRepository.save(Tarifa.builder()
                .tipo(TipoTarifa.CLIENTE)
                .codigo("SERASA")
                .nome("Consulta Serasa")
                .valor(new BigDecimal("10.00"))
                .ativa(true)
                .build());

        tarifaRepository.save(Tarifa.builder()
                .tipo(TipoTarifa.GERAL)
                .codigo("TAC")
                .nome("TAC")
                .valor(new BigDecimal("100.00"))
                .ativa(true)
                .build());

        tarifaRepository.save(Tarifa.builder()
                .tipo(TipoTarifa.GERAL)
                .codigo("TED")
                .nome("TED")
                .valor(new BigDecimal("50.00"))
                .ativa(true)
                .build());

        // Inserir feriados
        feriadoRepository.save(Feriado.builder()
                .data(LocalDate.of(2026, 1, 1))
                .nome("Ano Novo")
                .tipo(TipoFeriado.NACIONAL)
                .ativo(true)
                .build());

        feriadoRepository.save(Feriado.builder()
                .data(LocalDate.of(2026, 12, 25))
                .nome("Natal")
                .tipo(TipoFeriado.NACIONAL)
                .ativo(true)
                .build());
    }

    @Test
    @Order(1)
    @DisplayName("Cenário Completo: NF-e com 3 títulos")
    void cenarioCompletoNFeComTresTitulos() {
        // Arrange - Dados da NF-e
        BigDecimal valorPorTitulo = new BigDecimal("6500.00");
        int quantidadeTitulos = 3;

        // Act - Calcular tarifas
        CalculoTarifasResult tarifas = tarifaService.calcularTarifas(quantidadeTitulos, true);
        // Assert - Tarifas
        assertEquals(new BigDecimal("7.50"), tarifas.getTarifasPorDocumento());
        assertEquals(new BigDecimal("10.00"), tarifas.getTarifasPorCliente());
        assertEquals(new BigDecimal("150.00"), tarifas.getTarifasGerais());
        assertEquals(new BigDecimal("167.50"), tarifas.getValorTotal());
    }

    @Test
    @Order(2)
    @DisplayName("Calcular D+2 com obstáculo de fim de semana")
    void calcularD2ComObstaculoFimDeSemana() {
        // Arrange
        LocalDate sexta = LocalDate.of(2026, 1, 2); // Sexta

        // Act
        LocalDate resultado = diaUtilService.calcularProximoDiaUtil(sexta, 2);

        // Assert
        LocalDate esperado = LocalDate.of(2026, 1, 6); // Terça (pula sáb/dom)
        assertEquals(esperado, resultado);
    }

    @Test
    @Order(3)
    @DisplayName("Calcular D+2 com feriado (Ano Novo)")
    void calcularD2ComFeriado() {
        // Arrange
        LocalDate dia30Dez = LocalDate.of(2025, 12, 30); // Terça

        // Act
        LocalDate resultado = diaUtilService.calcularProximoDiaUtil(dia30Dez, 2, null, null);

        // Assert
        LocalDate esperado = LocalDate.of(2026, 1, 2); // Sexta (01/01 é feriado)
        assertEquals(esperado, resultado);
    }

    @Test
    @Order(4)
    @DisplayName("Verificar se Natal é feriado")
    void verificarSeNatalEhFeriado() {
        // Arrange
        LocalDate natal = LocalDate.of(2026, 12, 25);

        // Act
        boolean ehFeriado = diaUtilService.isFeriado(natal, null, null);

        // Assert
        assertTrue(ehFeriado);
    }

    @Test
    @Order(5)
    @DisplayName("Calcular dias úteis entre datas")
    void calcularDiasUteisEntreDatas() {
        // Arrange
        LocalDate inicio = LocalDate.of(2026, 1, 5); // Segunda
        LocalDate fim = LocalDate.of(2026, 1, 16); // Sexta (2 semanas depois)

        // Act
        int diasUteis = diaUtilService.calcularDiasUteis(inicio, fim, null, null);

        // Assert
        assertEquals(10, diasUteis); // 2 semanas = 10 dias úteis
    }
}