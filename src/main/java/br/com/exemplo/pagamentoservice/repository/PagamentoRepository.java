package br.com.exemplo.pagamentoservice.repository;

import br.com.exemplo.pagamentoservice.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fornece as operações de persistência da entidade Pagamento.
 *
 * <p>O Spring Data JPA cria automaticamente a implementação
 * desta interface durante a inicialização da aplicação.</p>
 */
public interface PagamentoRepository
        extends JpaRepository<Pagamento, Long> {

    /**
     * Localiza o pagamento criado a partir de determinado evento Kafka.
     *
     * @param eventoId identificador único do evento
     * @return pagamento encontrado ou Optional vazio
     */
    Optional<Pagamento> findByEventoId(
            UUID eventoId
    );

    /**
     * Localiza o pagamento associado a determinado pedido.
     *
     * @param pedidoId identificador do pedido no monólito
     * @return pagamento encontrado ou Optional vazio
     */
    Optional<Pagamento> findByPedidoId(
            Long pedidoId
    );

    /**
     * Lista os pagamentos começando pelos mais recentes.
     *
     * @return pagamentos ordenados pela data de criação
     */
    List<Pagamento> findAllByOrderByCriadoEmDesc();
}