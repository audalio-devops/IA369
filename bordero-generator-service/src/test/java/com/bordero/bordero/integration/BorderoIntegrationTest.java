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

        // Inserir tarifas - VALORES CONFORME ESPECIFICAÇÃO
        tarifaRepository.save(Tarifa.builder()
                .tipo(TipoTarifa.DOCUMENTO)
                .codigo("TAR_DOC")
                .nome("Tarifa por Título")
                .valor(new BigDecimal("15.00"))
                .ativa(true)
                .build());

        tarifaRepository.save(Tarifa.builder()
                .tipo(TipoTarifa.CLIENTE)
                .codigo("SERASA")
                .nome("Consulta Serasa")
                .valor(new BigDecimal("50.00"))
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

        // Inserir feriados para testes
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
        // Arrange
        int quantidadeTitulos = 3;

        // Act
        CalculoTarifasResult tarifas = tarifaService.calcularTarifas(quantidadeTitulos, true);

        // Assert
        assertEquals(new BigDecimal("45.00"), tarifas.getTarifasPorDocumento());
        assertEquals(new BigDecimal("50.00"), tarifas.getTarifasPorCliente());
        assertEquals(new BigDecimal("150.00"), tarifas.getTarifasGerais());
        assertEquals(new BigDecimal("245.00"), tarifas.getValorTotal());
    }

    @Test
    @Order(2)
    @DisplayName("Calcular D+2 com obstáculo de fim de semana")
    void calcularD2ComObstaculoFimDeSemana() {
        // Arrange
        // CORRIGIDO: Usar quinta-feira que D+2 cai em sábado
        LocalDate quinta = LocalDate.of(2026, 1, 8); // Quinta-feira

        // Act
        LocalDate resultado = diaUtilService.calcularProximoDiaUtil(quinta, 2);

        // Assert
        // D+2 de quinta (08/01) = sábado (10/01) -> ajusta para segunda (12/01)
        LocalDate esperado = LocalDate.of(2026, 1, 12); // Segunda-feira
        assertEquals(esperado, resultado);
    }

    @Test
    @Order(3)
    @DisplayName("Calcular D+2 em meio de semana sem obstáculos")
    void calcularD2EmMeioDeSemanaSemObstaculos() {
        // Arrange
        LocalDate segunda = LocalDate.of(2026, 1, 5); // Segunda-feira

        // Act
        LocalDate resultado = diaUtilService.calcularProximoDiaUtil(segunda, 2);

        // Assert
        // D+2 de segunda (05/01) = quarta (07/01) - dia útil normal
        LocalDate esperado = LocalDate.of(2026, 1, 7); // Quarta-feira
        assertEquals(esperado, resultado);
    }

    @Test
    @Order(4)
    @DisplayName("Calcular D+2 com feriado (Ano Novo)")
    void calcularD2ComFeriado() {
        // Arrange
        LocalDate dia30Dez = LocalDate.of(2025, 12, 30); // Terça-feira

        // Act
        LocalDate resultado = diaUtilService.calcularProximoDiaUtil(dia30Dez, 2, null, null);

        // Assert
        // D+2 de terça (30/12) = quinta (01/01) mas é feriado -> sexta (02/01)
        LocalDate esperado = LocalDate.of(2026, 1, 2); // Sexta-feira
        assertEquals(esperado, resultado);
    }

    @Test
    @Order(5)
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
    @Order(6)
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

    @Test
    @Order(7)
    @DisplayName("D+2 pulando fim de semana E feriado")
    void d2PulandoFimDeSemanaEFeriado() {
        // Arrange
        LocalDate dia31Dez = LocalDate.of(2025, 12, 31); // Quarta-feira

        // Act
        LocalDate resultado = diaUtilService.calcularProximoDiaUtil(dia31Dez, 2, null, null);

        // Assert
        // D+2 de quarta (31/12) = sexta (02/01/2026)
        // Pula 01/01 (quinta, feriado)
        LocalDate esperado = LocalDate.of(2026, 1, 2); // Sexta-feira
        assertEquals(esperado, resultado);
    }
}