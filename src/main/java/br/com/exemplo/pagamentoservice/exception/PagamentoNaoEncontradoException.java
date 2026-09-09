package br.com.exemplo.pagamentoservice.exception;

/**
 * Exceção lançada quando procuramos um pagamento
 * que não existe no banco deste microserviço.
 */
public class PagamentoNaoEncontradoException extends RuntimeException {

    public PagamentoNaoEncontradoException(Long pagamentoId) {
        super("Pagamento não encontrado: " + pagamentoId);
    }

    /**
     * Usamos uma fábrica para diferenciar a busca pelo pedido
     * da busca pelo identificador do próprio pagamento.
     */
    public static PagamentoNaoEncontradoException porPedido(Long pedidoId) {
        return new PagamentoNaoEncontradoException(
                "Pagamento não encontrado para o pedido: " + pedidoId
        );
    }

    private PagamentoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}