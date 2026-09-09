package br.com.exemplo.pagamentoservice.entity;

/**
 * Estados possíveis de um pagamento no laboratório.
 */
public enum StatusPagamento {

    /**
     * Pagamento criado, mas ainda não processado.
     */
    PENDENTE,

    /**
     * Pagamento autorizado.
     */
    APROVADO,

    /**
     * Pagamento não autorizado.
     */
    RECUSADO
}