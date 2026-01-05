package com.bordero.bordero.domain.model;

public enum TipoTarifa {
    DOCUMENTO,    // Cobrada por título
    CLIENTE,      // Cobrada uma vez por cliente (ex: consulta Serasa)
    GERAL         // Cobrada uma vez no borderô (ex: TAC, TED)
}