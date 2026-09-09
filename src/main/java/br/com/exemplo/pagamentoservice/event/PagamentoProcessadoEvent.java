package br.com.exemplo.pagamentoservice.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado quando um pagamento é aprovado ou recusado.
 *
 * Outros microserviços poderão consumir esse contrato sem acessar
 * diretamente o banco de dados do pagamento-service.
 */
public record PagamentoProcessadoEvent(
        UUID eventoId,
        Long pagamentoId,
        Long pedidoId,
        String status,
        String motivoRecusa,
        Instant ocorridoEm
) {
}