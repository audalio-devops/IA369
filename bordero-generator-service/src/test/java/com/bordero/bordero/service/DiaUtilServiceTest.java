package com.bordero.bordero.service;

import com.bordero.bordero.domain.model.Feriado;
import com.bordero.bordero.domain.model.TipoFeriado;
import com.bordero.bordero.repository.FeriadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do DiaUtilService")
class DiaUtilServiceTest {

    @Mock
    private FeriadoRepository feriadoRepository;

    @InjectMocks
    private DiaUtilService diaUtilService;

    @BeforeEach
    void setUp() {
        // Configurar feriados mock
        when(feriadoRepository.findByDataAndLocalidade(any(), any(), any()))
                .thenReturn(Collections.emptyList());
    }


    @Test
    @DisplayName("Deve identificar sábado como fim de semana")
    void deveIdentificarSabadoComoFimDeSemana() {
        // Arrange
        LocalDate sabado = LocalDate.of(2026, 1, 3); // Sábado

        // Act
        boolean result = diaUtilService.isFimDeSemana(sabado);

        // Assert
        assertTrue(result);
        assertEquals(DayOfWeek.SATURDAY, sabado.getDayOfWeek());
    }

    @Test
    @DisplayName("Deve identificar domingo como fim de semana")
    void deveIdentificarDomingoComoFimDeSemana() {
        // Arrange
        LocalDate domingo = LocalDate.of(2026, 1, 4); // Domingo

        // Act
        boolean result = diaUtilService.isFimDeSemana(domingo);

        // Assert
        assertTrue(result);
        assertEquals(DayOfWeek.SUNDAY, domingo.getDayOfWeek());
    }

    @Test
    @DisplayName("Deve identificar segunda-feira como dia útil (sem feriado)")
    void deveIdentificarSegundaComoNaoFimDeSemana() {
        // Arrange
        LocalDate segunda = LocalDate.of(2026, 1, 5); // Segunda

        // Act
        boolean result = diaUtilService.isFimDeSemana(segunda);

        // Assert
        assertFalse(result);
        assertEquals(DayOfWeek.MONDAY, segunda.getDayOfWeek());
    }

    @Test
    @DisplayName("D+2 em sexta deve ir para terça (pula fim de semana)")
    void d2EmSextaDeveIrParaTerca() {
        // Arrange
        LocalDate sexta = LocalDate.of(2026, 1, 2); // Sexta-feira
        int float_dias = 2;

        // Act
        LocalDate resultado = diaUtilService.calcularProximoDiaUtil(sexta, float_dias);

        // Assert
        LocalDate esperado = LocalDate.of(2026, 1, 6); // Terça após pular fim de semana
        assertEquals(esperado, resultado);
        assertEquals(DayOfWeek.TUESDAY, resultado.getDayOfWeek());
    }

    @Test
    @DisplayName("D+2 em quinta deve ir para segunda (vencimento cai no sábado)")
    void d2EmQuintaDeveIrParaSegunda() {
        // Arrange
        LocalDate quinta = LocalDate.of(2026, 1, 1); // Quinta-feira (Ano Novo é feriado, mas teste sem feriado)
        LocalDate dataBase = LocalDate.of(2026, 1, 8); // Quinta sem feriado
        int float_dias = 2;

        // Act
        LocalDate resultado = diaUtilService.calcularProximoDiaUtil(dataBase, float_dias);

        // Assert
        // D+2 de quinta (08/01) = sábado (10/01) -> vai para segunda (12/01)
        LocalDate esperado = LocalDate.of(2026, 1, 12); // Segunda
        assertEquals(esperado, resultado);
        assertEquals(DayOfWeek.MONDAY, resultado.getDayOfWeek());
    }

    @Test
    @DisplayName("D+2 em dia útil sem obstáculos deve retornar o próprio dia")
    void d2EmDiaUtilSemObstaculosDeveRetornarOProprioDia() {
        // Arrange
        LocalDate segunda = LocalDate.of(2026, 1, 5); // Segunda
        int float_dias = 2;

        // Act
        LocalDate resultado = diaUtilService.calcularProximoDiaUtil(segunda, float_dias);

        // Assert
        LocalDate esperado = LocalDate.of(2026, 1, 7); // Quarta
        assertEquals(esperado, resultado);
        assertEquals(DayOfWeek.WEDNESDAY, resultado.getDayOfWeek());
    }

    @Test
    @DisplayName("Deve pular feriado nacional - Ano Novo")
    void devePularFeriadoNacional() {
        // Arrange
        LocalDate dia30Dez = LocalDate.of(2025, 12, 30); // Terça
        int float_dias = 2;

        // Mock: 01/01/2026 é feriado
        Feriado anoNovo = Feriado.builder()
                .data(LocalDate.of(2026, 1, 1))
                .nome("Ano Novo")
                .tipo(TipoFeriado.NACIONAL)
                .ativo(true)
                .build();

        when(feriadoRepository.findByDataAndLocalidade(
                eq(LocalDate.of(2026, 1, 1)), anyString(), anyString()))
                .thenReturn(List.of(anoNovo));

        // Act
        LocalDate resultado = diaUtilService.calcularProximoDiaUtil(dia30Dez, float_dias, null, null);

        // Assert
        // D+2 de 30/12 = 01/01 (feriado) -> próximo dia útil = 02/01
        LocalDate esperado = LocalDate.of(2026, 1, 2); // Sexta
        assertEquals(esperado, resultado);
    }

    @Test
    @DisplayName("Deve pular feriado + fim de semana")
    void devePularFeriadoEFimDeSemana() {
        // Arrange
        LocalDate dia31Dez = LocalDate.of(2025, 12, 31); // Quarta
        int float_dias = 2;

        // Mock: 01/01/2026 é feriado, 02/01 quinta, 03/01 sexta, 04/01 sábado, 05/01 domingo
        Feriado anoNovo = Feriado.builder()
                .data(LocalDate.of(2026, 1, 1))
                .nome("Ano Novo")
                .tipo(TipoFeriado.NACIONAL)
                .ativo(true)
                .build();

        when(feriadoRepository.findByDataAndLocalidade(
                eq(LocalDate.of(2026, 1, 1)), anyString(), anyString()))
                .thenReturn(List.of(anoNovo));

        // Act
        LocalDate resultado = diaUtilService.calcularProximoDiaUtil(dia31Dez, float_dias, null, null);

        // Assert
        LocalDate esperado = LocalDate.of(2026, 1, 2); // Sexta
        assertEquals(esperado, resultado);
    }

    @Test
    @DisplayName("Deve calcular dias úteis entre duas datas")
    void deveCalcularDiasUteisEntreDuasDatas() {
        // Arrange
        LocalDate inicio = LocalDate.of(2026, 1, 5); // Segunda
        LocalDate fim = LocalDate.of(2026, 1, 9); // Sexta

        // Act
        int diasUteis = diaUtilService.calcularDiasUteis(inicio, fim, null, null);

        // Assert
        assertEquals(5, diasUteis); // Seg, Ter, Qua, Qui, Sex
    }

    @Test
    @DisplayName("Deve calcular dias úteis pulando fim de semana")
    void deveCalcularDiasUteisPulandoFimDeSemana() {
        // Arrange
        LocalDate inicio = LocalDate.of(2026, 1, 2); // Sexta
        LocalDate fim = LocalDate.of(2026, 1, 6); // Terça

        // Act
        int diasUteis = diaUtilService.calcularDiasUteis(inicio, fim, null, null);

        // Assert
        assertEquals(3, diasUteis); // Sexta, Segunda, Terça (pula Sáb e Dom)
    }

    @Test
    @DisplayName("Deve calcular float real considerando fim de semana")
    void deveCalcularFloatRealConsiderandoFimDeSemana() {
        // Arrange
        LocalDate dataBase = LocalDate.of(2026, 1, 8); // Quinta
        int floatBase = 2;

        // Act
        int floatReal = diaUtilService.calcularDiasAteProximoDiaUtil(dataBase, floatBase, null, null);

        // Assert
        // D+2 de quinta = sábado -> ajusta para segunda = D+4
        assertEquals(4, floatReal);
    }

    @Test
    @DisplayName("Deve manter float quando não há obstáculos")
    void deveManterFloatQuandoNaoHaObstaculos() {
        // Arrange
        LocalDate dataBase = LocalDate.of(2026, 1, 5); // Segunda
        int floatBase = 2;

        // Act
        int floatReal = diaUtilService.calcularDiasAteProximoDiaUtil(dataBase, floatBase, null, null);

        // Assert
        // D+2 de segunda = quarta (dia útil) -> mantém D+2
        assertEquals(2, floatReal);
    }

    @Test
    @DisplayName("Deve trabalhar com LocalDateTime")
    void deveTrabalharComLocalDateTime() {
        // Arrange
        LocalDateTime dataBase = LocalDateTime.of(2026, 1, 8, 14, 30); // Quinta 14:30
        int float_dias = 2;

        // Act
        LocalDateTime resultado = diaUtilService.calcularProximoDiaUtil(dataBase, float_dias, null, null);

        // Assert
        assertEquals(LocalDate.of(2026, 1, 12), resultado.toLocalDate()); // Segunda
        assertEquals(14, resultado.getHour());
        assertEquals(30, resultado.getMinute());
    }
}
