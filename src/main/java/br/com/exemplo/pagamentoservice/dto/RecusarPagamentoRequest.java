package br.com.exemplo.pagamentoservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo JSON recebido na recusa de um pagamento.
 *
 * O motivo pertence à requisição e não deve ser recebido
 * diretamente como uma String solta no controller.
 */
public record RecusarPagamentoRequest(

        @NotBlank(message = "O motivo da recusa é obrigatório")
        String motivo
) {
}