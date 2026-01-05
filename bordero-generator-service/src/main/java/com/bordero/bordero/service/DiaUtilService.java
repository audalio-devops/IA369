package com.bordero.bordero.service;

import com.bordero.bordero.repository.FeriadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaUtilService {

    private final FeriadoRepository feriadoRepository;

    /**
     * Calcula o próximo dia útil a partir de uma data + dias corridos
     *
     * @param dataBase Data base para o cálculo
     * @param diasCorridos Dias corridos a adicionar (ex: 2 para D+2)
     * @param uf UF para considerar feriados estaduais
     * @param codigoMunicipio Código do município para feriados municipais
     * @return Próximo dia útil
     */
    public LocalDate calcularProximoDiaUtil(LocalDate dataBase, int diasCorridos,
                                            String uf, String codigoMunicipio) {
        LocalDate data = dataBase.plusDays(diasCorridos);

        while (!isDiaUtil(data, uf, codigoMunicipio)) {
            data = data.plusDays(1);
        }

        log.debug("Data base: {}, D+{} = {}", dataBase, diasCorridos, data);
        return data;
    }

    /**
     * Calcula o próximo dia útil (versão simplificada - considera apenas feriados nacionais)
     */
    public LocalDate calcularProximoDiaUtil(LocalDate dataBase, int diasCorridos) {
        return calcularProximoDiaUtil(dataBase, diasCorridos, null, null);
    }

    /**
     * Calcula o próximo dia útil a partir de data/hora
     */
    public LocalDateTime calcularProximoDiaUtil(LocalDateTime dataBase, int diasCorridos,
                                                String uf, String codigoMunicipio) {
        LocalDate dataUtil = calcularProximoDiaUtil(dataBase.toLocalDate(), diasCorridos, uf, codigoMunicipio);
        return dataUtil.atTime(dataBase.toLocalTime());
    }

    /**
     * Verifica se uma data é dia útil
     */
    public boolean isDiaUtil(LocalDate data, String uf, String codigoMunicipio) {
        // Verifica se é fim de semana
        if (isFimDeSemana(data)) {
            return false;
        }

        // Verifica se é feriado
        return !isFeriado(data, uf, codigoMunicipio);
    }

    /**
     * Verifica se é fim de semana
     */
    public boolean isFimDeSemana(LocalDate data) {
        DayOfWeek diaSemana = data.getDayOfWeek();
        return diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY;
    }

    /**
     * Verifica se é feriado
     */
    public boolean isFeriado(LocalDate data, String uf, String codigoMunicipio) {
        var feriados = feriadoRepository.findByDataAndLocalidade(
                data,
                uf != null ? uf : "",
                codigoMunicipio != null ? codigoMunicipio : ""
        );
        return !feriados.isEmpty();
    }

    /**
     * Calcula a quantidade de dias úteis entre duas datas
     */
    public int calcularDiasUteis(LocalDate dataInicio, LocalDate dataFim,
                                 String uf, String codigoMunicipio) {
        int diasUteis = 0;
        LocalDate data = dataInicio;

        while (!data.isAfter(dataFim)) {
            if (isDiaUtil(data, uf, codigoMunicipio)) {
                diasUteis++;
            }
            data = data.plusDays(1);
        }

        return diasUteis;
    }

    /**
     * Calcula dias corridos até o próximo dia útil (para cálculo de float)
     */
    public int calcularDiasAteProximoDiaUtil(LocalDate dataBase, int floatBase,
                                             String uf, String codigoMunicipio) {
        LocalDate dataCalculada = dataBase.plusDays(floatBase);
        int diasAdicionais = 0;

        while (!isDiaUtil(dataCalculada, uf, codigoMunicipio)) {
            diasAdicionais++;
            dataCalculada = dataCalculada.plusDays(1);
        }

        return floatBase + diasAdicionais;
    }
}