package br.com.exemplo.pagamentoservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa o contrato da mensagem recebida do tópico pedidos-criados.
 *
 * <p>O monólito possui uma classe equivalente, mas o pagamento-service
 * mantém seu próprio contrato para não depender do código-fonte de outra
 * aplicação.</p>
 *
 * <p>Os nomes e tipos dos campos precisam ser compatíveis com o JSON
 * publicado pelo ProjetoSpringBoot.</p>
 */
public record PedidoCriadoEvent(

        /**
         * Identificador único do evento.
         *
         * <p>Será utilizado para impedir que uma mensagem repetida
         * crie outro pagamento.</p>
         */
        UUID eventoId,

        /**
         * Identificador do pedido criado no monólito.
         */
        Long pedidoId,

        /**
         * Identificador do cliente responsável pelo pedido.
         */
        Long clienteId,

        /**
         * Valor total do pedido no momento da criação.
         */
        BigDecimal valor,

        /**
         * Instante em que o pedido foi criado no sistema de origem.
         */
        Instant ocorridoEm

) {
}