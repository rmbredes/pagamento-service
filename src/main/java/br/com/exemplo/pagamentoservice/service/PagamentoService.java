package br.com.exemplo.pagamentoservice.service;

import br.com.exemplo.pagamentoservice.entity.Pagamento;
import br.com.exemplo.pagamentoservice.event.PedidoCriadoEvent;
import br.com.exemplo.pagamentoservice.messaging.producer.PagamentoProcessadoProducer;
import br.com.exemplo.pagamentoservice.repository.PagamentoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import br.com.exemplo.pagamentoservice.exception.PagamentoNaoEncontradoException;

import java.util.List;

import br.com.exemplo.pagamentoservice.event.PagamentoProcessadoEvent;
import br.com.exemplo.pagamentoservice.messaging.producer.PagamentoProcessadoProducer;

import java.time.Instant;
import java.util.UUID;

/**
 * Executa as regras relacionadas aos pagamentos.
 *
 * <p>O consumer conhece Kafka. O repository conhece o banco.
 * Este service coordena os dois lados sem depender dos detalhes
 * internos dessas tecnologias.</p>
 */
@Service
public class PagamentoService {

    /**
     * Logger utilizado para acompanhar o processamento.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PagamentoService.class);

    /**
     * Repositório usado para consultar e persistir pagamentos.
     */
    private final PagamentoRepository pagamentoRepository;

    private final PagamentoProcessadoProducer pagamentoProcessadoProducer;

    /**
     * Recebe o repository por injeção de dependência.
     *
     * @param pagamentoRepository acesso aos pagamentos persistidos
     */
    public PagamentoService(
            PagamentoRepository pagamentoRepository, PagamentoProcessadoProducer pagamentoProcessadoProducer
    ) {
        this.pagamentoRepository = pagamentoRepository;
        this.pagamentoProcessadoProducer = pagamentoProcessadoProducer;
    }

    /**
     * Registra um pagamento para o pedido recebido pelo Kafka.
     *
     * <p>A operação é idempotente: receber novamente o mesmo evento
     * ou outro evento referente ao mesmo pedido não cria outro
     * registro.</p>
     *
     * @param evento evento publicado pelo monólito
     * @return pagamento existente ou recém-criado
     */
    @Transactional
    public Pagamento registrarPedidoCriado(
            PedidoCriadoEvent evento
    ) {

        // Impede que dados inválidos cheguem à tabela.
        validarEvento(evento);

        /*
         * Primeira proteção contra duplicidade:
         * verifica se esse evento específico já foi processado.
         */
        Optional<Pagamento> pagamentoDoEvento =
                pagamentoRepository.findByEventoId(
                        evento.eventoId()
                );

        if (pagamentoDoEvento.isPresent()) {

            Pagamento pagamentoExistente =
                    pagamentoDoEvento.get();

            LOGGER.info(
                    "Evento já processado: eventoId={}, pagamentoId={}, pedidoId={}",
                    evento.eventoId(),
                    pagamentoExistente.getId(),
                    pagamentoExistente.getPedidoId()
            );

            return pagamentoExistente;
        }

        /*
         * Segunda proteção:
         * nesta versão existe apenas um pagamento por pedido.
         */
        Optional<Pagamento> pagamentoDoPedido =
                pagamentoRepository.findByPedidoId(
                        evento.pedidoId()
                );

        if (pagamentoDoPedido.isPresent()) {

            Pagamento pagamentoExistente =
                    pagamentoDoPedido.get();

            LOGGER.info(
                    "Pedido já possui pagamento: pedidoId={}, pagamentoId={}",
                    evento.pedidoId(),
                    pagamentoExistente.getId()
            );

            return pagamentoExistente;
        }

        // Converte o evento recebido em uma entidade persistente.
        Pagamento pagamento =
                new Pagamento(
                        evento.eventoId(),
                        evento.pedidoId(),
                        evento.clienteId(),
                        evento.valor(),
                        evento.ocorridoEm()
                );

        // Persiste o novo pagamento no banco exclusivo do serviço.
        Pagamento pagamentoSalvo =
                pagamentoRepository.save(
                        pagamento
                );

        LOGGER.info(
                "Pagamento criado: pagamentoId={}, pedidoId={}, eventoId={}, status={}",
                pagamentoSalvo.getId(),
                pagamentoSalvo.getPedidoId(),
                pagamentoSalvo.getEventoId(),
                pagamentoSalvo.getStatus()
        );

        return pagamentoSalvo;
    }

    /**
     * Valida os dados mínimos necessários para criar um pagamento.
     *
     * @param evento evento recebido pelo Kafka
     */
    private void validarEvento(
            PedidoCriadoEvent evento
    ) {

        if (evento == null) {
            throw new IllegalArgumentException(
                    "O evento de pedido criado deve ser informado"
            );
        }

        if (evento.eventoId() == null) {
            throw new IllegalArgumentException(
                    "O eventoId deve ser informado"
            );
        }

        if (evento.pedidoId() == null) {
            throw new IllegalArgumentException(
                    "O pedidoId deve ser informado"
            );
        }

        if (evento.clienteId() == null) {
            throw new IllegalArgumentException(
                    "O clienteId deve ser informado"
            );
        }

        if (evento.valor() == null
                || evento.valor().compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "O valor do pedido deve ser maior que zero"
            );
        }

        if (evento.ocorridoEm() == null) {
            throw new IllegalArgumentException(
                    "O instante de criação do pedido deve ser informado"
            );
        }
    }

    /**
     * Lista todos os pagamentos, dos mais recentes para os mais antigos.
     *
     * readOnly = true informa que esta transação será apenas de leitura.
     */
    @Transactional(readOnly = true)
    public List<Pagamento> listar() {
        return pagamentoRepository.findAllByOrderByCriadoEmDesc();
    }

    /**
     * Busca um pagamento pelo ID interno do pagamento-service.
     */
    @Transactional(readOnly = true)
    public Pagamento buscarPorId(Long pagamentoId) {
        return pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() ->
                        new PagamentoNaoEncontradoException(pagamentoId)
                );
    }

    /**
     * Busca o pagamento relacionado a determinado pedido.
     */
    @Transactional(readOnly = true)
    public Pagamento buscarPorPedidoId(Long pedidoId) {
        return pagamentoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() ->
                        PagamentoNaoEncontradoException.porPedido(pedidoId)
                );
    }

    /**
     * Aprova um pagamento que ainda está PENDENTE.
     *
     * Não chamamos save() porque a entidade foi carregada dentro de uma
     * transação. O JPA identifica a alteração e executa o UPDATE ao final.
     * Esse comportamento é chamado de dirty checking.
     */
    @Transactional
    public Pagamento aprovar(Long pagamentoId) {
        Pagamento pagamento = buscarPorId(pagamentoId);

        pagamento.aprovar();
        PagamentoProcessadoEvent evento =
                criarEventoPagamentoProcessado(pagamento);

        pagamentoProcessadoProducer.publicar(evento);

        LOGGER.info(
                "Pagamento aprovado: pagamentoId={}, pedidoId={}",
                pagamento.getId(),
                pagamento.getPedidoId()
        );

        return pagamento;
    }

    /**
     * Recusa um pagamento PENDENTE e registra o motivo.
     */
    @Transactional
    public Pagamento recusar(Long pagamentoId, String motivo) {
        Pagamento pagamento = buscarPorId(pagamentoId);

        pagamento.recusar(motivo);

        PagamentoProcessadoEvent evento =
                criarEventoPagamentoProcessado(pagamento);

        pagamentoProcessadoProducer.publicar(evento);

        LOGGER.info(
                "Pagamento recusado: pagamentoId={}, pedidoId={}, motivo={}",
                pagamento.getId(),
                pagamento.getPedidoId(),
                motivo
        );

        return pagamento;
    }

    /**
     * Monta o evento público a partir do estado atual do pagamento.
     */
    private PagamentoProcessadoEvent criarEventoPagamentoProcessado(
            Pagamento pagamento
    ) {
        return new PagamentoProcessadoEvent(
                UUID.randomUUID(),
                pagamento.getId(),
                pagamento.getPedidoId(),
                pagamento.getStatus().name(),
                pagamento.getMotivoRecusa(),
                Instant.now()
        );
    }
}