package br.com.exemplo.pagamentoservice.dto;

import java.time.Instant;

/**
 * Formato padrão dos erros devolvidos pela API.
 *
 * Independentemente da origem do erro, o cliente receberá
 * sempre uma estrutura previsível.
 */
public record ErroResponse(
        Instant dataHora,
        int status,
        String erro,
        String mensagem,
        String caminho
) {
}