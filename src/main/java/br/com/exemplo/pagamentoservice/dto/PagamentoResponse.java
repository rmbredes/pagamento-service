package br.com.exemplo.pagamentoservice.dto;

import br.com.exemplo.pagamentoservice.entity.Pagamento;
import br.com.exemplo.pagamentoservice.entity.StatusPagamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO devolvido pelos endpoints do pagamento-service.
 *
 * A entidade representa o registro dentro do banco.
 * Este DTO representa os dados que nossa API expõe externamente.
 */
public record PagamentoResponse(
        Long id,
        UUID eventoId,
        Long pedidoId,
        Long clienteId,
        BigDecimal valor,
        StatusPagamento status,
        String motivoRecusa,
        Instant pedidoCriadoEm,
        Instant criadoEm,
        Instant atualizadoEm,
        Instant processadoEm
) {

    /**
     * Converte uma entidade Pagamento para o DTO de resposta.
     *
     * Central mantém * Esse método central o mapeamento em um único lugar e evita
     * repetir vários getters dentro do controller.
     */
    public static PagamentoResponse from(Pagamento pagamento) {
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getEventoId(),
                pagamento.getPedidoId(),
                pagamento.getClienteId(),
                pagamento.getValor(),
                pagamento.getStatus(),
                pagamento.getMotivoRecusa(),
                pagamento.getPedidoCriadoEm(),
                pagamento.getCriadoEm(),
                pagamento.getAtualizadoEm(),
                pagamento.getProcessadoEm()
        );
    }
}