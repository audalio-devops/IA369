package com.bordero.bordero.utils;

import com.bordero.bordero.domain.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TestDataBuilder {

    public static Bordero criarBorderoMock() {
        Bordero bordero = Bordero.builder()
                .id(1L)
                .numeroBordero("BOR123456")
                .dataGeracao(LocalDateTime.now())
                .cnpjCedente("16554601000186")
                .nomeCedente("VICOLACCI")
                .cnpjFundo("09609468000152")
                .nomeFundo("FIDC MACRO FUND")
                .valorBruto(new BigDecimal("19500.00"))
                .valorDesagio(new BigDecimal("1911.00"))
                .valorTarifas(new BigDecimal("167.50"))
                .valorLiquido(new BigDecimal("17421.50"))
                .quantidadeTitulos(3)
                .prazoMedio(196)
                .status(StatusBordero.GERADO)
                .build();

        bordero.addTitulo(criarTituloMock(bordero, 1, LocalDateTime.of(2026, 7, 15, 0, 0)));
        bordero.addTitulo(criarTituloMock(bordero, 2, LocalDateTime.of(2026, 8, 15, 0, 0)));
        bordero.addTitulo(criarTituloMock(bordero, 3, LocalDateTime.of(2026, 9, 15, 0, 0)));

        return bordero;
    }

    public static TituloBordero criarTituloMock(Bordero bordero, int numero, LocalDateTime vencimento) {
        return TituloBordero.builder()
                .bordero(bordero)
                .nfeId(1L)
                .duplicataId((long) numero)
                .chaveAcessoNFe("35230616554601000186550010000024761699677603")
                .numeroNFe("2476")
                .numeroDuplicata(String.format("%03d", numero))
                .dataVencimento(vencimento)
                .valorBruto(new BigDecimal("6500.00"))
                .diasParaVencimento(196)
                .taxaDesagio(new BigDecimal("1.50"))
                .valorDesagio(new BigDecimal("637.00"))
                .valorLiquido(new BigDecimal("5860.50"))
                .cnpjSacado("13771702000110")
                .nomeSacado("THAIS RODRIGUES")
                .build();
    }

    public static List<Tarifa> criarTarifasPadrao() {
        List<Tarifa> tarifas = new ArrayList<>();

        tarifas.add(Tarifa.builder()
                .tipo(TipoTarifa.DOCUMENTO)
                .codigo("TAR_DOC")
                .nome("Tarifa por Título")
                .valor(new BigDecimal("2.50"))
                .ativa(true)
                .build());

        tarifas.add(Tarifa.builder()
                .tipo(TipoTarifa.CLIENTE)
                .codigo("SERASA")
                .nome("Consulta Serasa")
                .valor(new BigDecimal("10.00"))
                .ativa(true)
                .build());

        tarifas.add(Tarifa.builder()
                .tipo(TipoTarifa.GERAL)
                .codigo("TAC")
                .nome("TAC")
                .valor(new BigDecimal("100.00"))
                .ativa(true)
                .build());

        tarifas.add(Tarifa.builder()
                .tipo(TipoTarifa.GERAL)
                .codigo("TED")
                .nome("TED")
                .valor(new BigDecimal("50.00"))
                .ativa(true)
                .build());

        return tarifas;
    }

    public static List<Feriado> criarFeriadosBasicos2026() {
        List<Feriado> feriados = new ArrayList<>();

        feriados.add(Feriado.builder()
                .data(LocalDate.of(2026, 1, 1))
                .nome("Ano Novo")
                .tipo(TipoFeriado.NACIONAL)
                .ativo(true)
                .build());

        feriados.add(Feriado.builder()
                .data(LocalDate.of(2026, 4, 21))
                .nome("Tiradentes")
                .tipo(TipoFeriado.NACIONAL)
                .ativo(true)
                .build());

        feriados.add(Feriado.builder()
                .data(LocalDate.of(2026, 5, 1))
                .nome("Dia do Trabalho")
                .tipo(TipoFeriado.NACIONAL)
                .ativo(true)
                .build());

        feriados.add(Feriado.builder()
                .data(LocalDate.of(2026, 9, 7))
                .nome("Independência")
                .tipo(TipoFeriado.NACIONAL)
                .ativo(true)
                .build());

        feriados.add(Feriado.builder()
                .data(LocalDate.of(2026, 12, 25))
                .nome("Natal")
                .tipo(TipoFeriado.NACIONAL)
                .ativo(true)
                .build());

        return feriados;
    }
}
