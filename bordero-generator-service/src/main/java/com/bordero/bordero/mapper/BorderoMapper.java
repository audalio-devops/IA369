package com.bordero.bordero.mapper;

import com.bordero.bordero.domain.model.Bordero;
import com.bordero.bordero.domain.model.TipoTitulo;
import com.bordero.bordero.domain.model.TituloBordero;
import com.bordero.bordero.dto.BorderoDTO;
import com.bordero.bordero.dto.TipoTituloDTO;
import com.bordero.bordero.dto.TituloBorderoDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para converter entidades em DTOs e vice-versa
 * Quebra o loop infinito de serialização JSON
 */
@Component
public class BorderoMapper {

    /**
     * Converte Bordero (entidade) para BorderoDTO
     */
    public BorderoDTO toDTO(Bordero bordero) {
        if (bordero == null) {
            return null;
        }

        BorderoDTO dto = BorderoDTO.builder()
                .id(bordero.getId())
                .numeroBordero(bordero.getNumeroBordero())
                .dataGeracao(bordero.getDataGeracao())
                .cnpjCedente(bordero.getCnpjCedente())
                .nomeCedente(bordero.getNomeCedente())
                .cnpjCliente(bordero.getCnpjCliente())
                .cnpjFundo(bordero.getCnpjFundo())
                .nomeFundo(bordero.getNomeFundo())
                .valorBruto(bordero.getValorBruto())
                .taxaDesagio(bordero.getTaxaDesagio())
                .valorDesagio(bordero.getValorDesagio())
                .valorTarifas(bordero.getValorTarifas())
                .valorLiquido(bordero.getValorLiquido())
                .tarifasDetalhamento(bordero.getTarifasDetalhamento())
                .quantidadeTitulos(bordero.getQuantidadeTitulos())
                .quantidadeSacados(bordero.getQuantidadeSacados())
                .prazoMedio(bordero.getPrazoMedio())
                .prazoMedioDiasUteis(bordero.getPrazoMedioDiasUteis())
                .vencimentoMenor(bordero.getVencimentoMenor())
                .vencimentoMaior(bordero.getVencimentoMaior())
                .status(bordero.getStatus())
                .dataCriacao(bordero.getDataCriacao())
                .dataAtualizacao(bordero.getDataAtualizacao())
                .build();

        // Converte os títulos (sem criar loop)
        if (bordero.getTitulos() != null) {
            dto.setTitulos(
                    bordero.getTitulos().stream()
                            .map(this::toTituloDTO)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    /**
     * Converte lista de Borderos para lista de DTOs
     */
    public List<BorderoDTO> toDTOList(List<Bordero> borderos) {
        if (borderos == null) {
            return List.of();
        }
        return borderos.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converte TituloBordero (entidade) para TituloBorderoDTO
     * IMPORTANTE: Não inclui o objeto Bordero completo, apenas IDs
     */
    public TituloBorderoDTO toTituloDTO(TituloBordero titulo) {
        if (titulo == null) {
            return null;
        }

        return TituloBorderoDTO.builder()
                .id(titulo.getId())
                .borderoId(titulo.getBordero() != null ? titulo.getBordero().getId() : null)
                .numeroBordero(titulo.getBordero() != null ? titulo.getBordero().getNumeroBordero() : null)
                .tipoTitulo(toTipoTituloDTO(titulo.getTipoTitulo()))
                .nfeId(titulo.getNfeId())
                .duplicataId(titulo.getDuplicataId())
                .chaveAcessoNFe(titulo.getChaveAcessoNFe())
                .numeroNFe(titulo.getNumeroNFe())
                .numeroDuplicata(titulo.getNumeroDuplicata())
                .dataVencimento(titulo.getDataVencimento())
                .dataCompensacao(titulo.getDataCompensacao())
                .diasParaVencimento(titulo.getDiasParaVencimento())
                .diasUteis(titulo.getDiasUteis())
                .prazoAdicional(titulo.getPrazoAdicional())
                .floatDias(titulo.getFloatDias())
                .valorBruto(titulo.getValorBruto())
                .taxaDesagio(titulo.getTaxaDesagio())
                .valorDesagio(titulo.getValorDesagio())
                .valorLiquido(titulo.getValorLiquido())
                .tarifaDocumento(titulo.getTarifaDocumento())
                .cnpjSacado(titulo.getCnpjSacado())
                .nomeSacado(titulo.getNomeSacado())
                .cnpjEmitente(titulo.getCnpjEmitente())
                .nomeEmitente(titulo.getNomeEmitente())
                .build();
    }

    /**
     * Converte TipoTitulo para TipoTituloDTO
     */
    public TipoTituloDTO toTipoTituloDTO(TipoTitulo tipoTitulo) {
        if (tipoTitulo == null) {
            return null;
        }

        return TipoTituloDTO.builder()
                .id(tipoTitulo.getId())
                .tipo(tipoTitulo.getTipo())
                .nome(tipoTitulo.getNome())
                .descricao(tipoTitulo.getDescricao())
                .ativo(tipoTitulo.getAtivo())
                .build();
    }

    /**
     * Converte lista de Títulos para lista de DTOs
     */
    public List<TituloBorderoDTO> toTituloDTOList(List<TituloBordero> titulos) {
        if (titulos == null) {
            return List.of();
        }
        return titulos.stream()
                .map(this::toTituloDTO)
                .collect(Collectors.toList());
    }
}