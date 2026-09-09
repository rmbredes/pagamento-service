package br.com.exemplo.pagamentoservice.service;

import br.com.exemplo.pagamentoservice.entity.Pagamento;
import br.com.exemplo.pagamentoservice.entity.StatusPagamento;
import br.com.exemplo.pagamentoservice.event.PagamentoProcessadoEvent;
import br.com.exemplo.pagamentoservice.event.PedidoCriadoEvent;
import br.com.exemplo.pagamentoservice.messaging.producer.PagamentoProcessadoProducer;
import br.com.exemplo.pagamentoservice.repository.PagamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários das principais regras do PagamentoService.
 *
 * Não carregamos o contexto Spring e não acessamos recursos externos.
 * Repositório e producer Kafka são substituídos por mocks.
 */
@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private PagamentoProcessadoProducer pagamentoProcessadoProducer;

    private PagamentoService pagamentoService;

    @BeforeEach
    void preparar() {
        /*
         * Criamos manualmente o service com suas dependências falsas.
         * Isso mantém os testes rápidos e fáceis de compreender.
         */
        pagamentoService = new PagamentoService(
                pagamentoRepository,
                pagamentoProcessadoProducer
        );
    }

    @Test
    void deveRegistrarPagamentoPendenteAoReceberNovoPedido() {
        PedidoCriadoEvent evento = criarEventoPedido();

        /*
         * Simulamos que ainda não existe pagamento para o evento
         * nem para o pedido.
         */
        when(pagamentoRepository.findByEventoId(evento.eventoId()))
                .thenReturn(Optional.empty());

        when(pagamentoRepository.findByPedidoId(evento.pedidoId()))
                .thenReturn(Optional.empty());

        /*
         * O mock devolve a própria entidade recebida pelo save().
         */
        when(pagamentoRepository.save(any(Pagamento.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Pagamento pagamento =
                pagamentoService.registrarPedidoCriado(evento);

        assertEquals(evento.eventoId(), pagamento.getEventoId());
        assertEquals(evento.pedidoId(), pagamento.getPedidoId());
        assertEquals(evento.clienteId(), pagamento.getClienteId());
        assertEquals(evento.valor(), pagamento.getValor());
        assertEquals(StatusPagamento.PENDENTE, pagamento.getStatus());

        verify(pagamentoRepository).save(any(Pagamento.class));

        /*
         * Registrar o pedido ainda não significa processar o pagamento.
         * Portanto, nada deve ser publicado no tópico de saída.
         */
        verifyNoInteractions(pagamentoProcessadoProducer);
    }

    @Test
    void naoDeveDuplicarPagamentoDoMesmoEvento() {
        PedidoCriadoEvent evento = criarEventoPedido();
        Pagamento pagamentoExistente = criarPagamentoPendente(evento);

        /*
         * Simulamos que o evento já foi processado anteriormente.
         */
        when(pagamentoRepository.findByEventoId(evento.eventoId()))
                .thenReturn(Optional.of(pagamentoExistente));

        Pagamento resultado =
                pagamentoService.registrarPedidoCriado(evento);

        /*
         * O service deve devolver o registro existente,
         * sem criar outro pagamento.
         */
        assertSame(pagamentoExistente, resultado);

        verify(pagamentoRepository, never())
                .save(any(Pagamento.class));

        verifyNoInteractions(pagamentoProcessadoProducer);
    }

    @Test
    void deveAprovarPagamentoEPublicarEvento() {
        PedidoCriadoEvent pedidoCriado = criarEventoPedido();
        Pagamento pagamento = criarPagamentoPendente(pedidoCriado);

        when(pagamentoRepository.findById(1L))
                .thenReturn(Optional.of(pagamento));

        Pagamento resultado = pagamentoService.aprovar(1L);

        assertEquals(StatusPagamento.APROVADO, resultado.getStatus());
        assertNotNull(resultado.getProcessadoEm());
        assertNull(resultado.getMotivoRecusa());

        /*
         * Capturamos o objeto enviado ao producer para conferir
         * o conteúdo do evento sem acessar um Kafka real.
         */
        ArgumentCaptor<PagamentoProcessadoEvent> captor =
                ArgumentCaptor.forClass(PagamentoProcessadoEvent.class);

        verify(pagamentoProcessadoProducer)
                .publicar(captor.capture());

        PagamentoProcessadoEvent eventoPublicado = captor.getValue();

        assertNotNull(eventoPublicado.eventoId());
        assertEquals(pagamento.getPedidoId(), eventoPublicado.pedidoId());
        assertEquals("APROVADO", eventoPublicado.status());
        assertNull(eventoPublicado.motivoRecusa());
        assertNotNull(eventoPublicado.ocorridoEm());
    }

    @Test
    void deveRecusarPagamentoEPublicarMotivo() {
        PedidoCriadoEvent pedidoCriado = criarEventoPedido();
        Pagamento pagamento = criarPagamentoPendente(pedidoCriado);

        when(pagamentoRepository.findById(2L))
                .thenReturn(Optional.of(pagamento));

        Pagamento resultado = pagamentoService.recusar(
                2L,
                "Pagamento não autorizado"
        );

        assertEquals(StatusPagamento.RECUSADO, resultado.getStatus());
        assertEquals(
                "Pagamento não autorizado",
                resultado.getMotivoRecusa()
        );
        assertNotNull(resultado.getProcessadoEm());

        ArgumentCaptor<PagamentoProcessadoEvent> captor =
                ArgumentCaptor.forClass(PagamentoProcessadoEvent.class);

        verify(pagamentoProcessadoProducer)
                .publicar(captor.capture());

        PagamentoProcessadoEvent eventoPublicado = captor.getValue();

        assertNotNull(eventoPublicado.eventoId());
        assertEquals(pagamento.getPedidoId(), eventoPublicado.pedidoId());
        assertEquals("RECUSADO", eventoPublicado.status());
        assertEquals(
                "Pagamento não autorizado",
                eventoPublicado.motivoRecusa()
        );
        assertNotNull(eventoPublicado.ocorridoEm());
    }

    /**
     * Cria um contrato de entrada reutilizado pelos testes.
     */
    private PedidoCriadoEvent criarEventoPedido() {
        return new PedidoCriadoEvent(
                UUID.randomUUID(),
                200L,
                7L,
                new BigDecimal("777.77"),
                Instant.parse("2026-09-08T13:27:24Z")
        );
    }

    /**
     * Cria uma entidade PENDENTE sem acessar o banco.
     */
    private Pagamento criarPagamentoPendente(
            PedidoCriadoEvent evento
    ) {
        return new Pagamento(
                evento.eventoId(),
                evento.pedidoId(),
                evento.clienteId(),
                evento.valor(),
                evento.ocorridoEm()
        );
    }
}