package br.com.exemplo.pagamentoservice.messaging.consumer;

import br.com.exemplo.pagamentoservice.entity.Pagamento;
import br.com.exemplo.pagamentoservice.event.PedidoCriadoEvent;
import br.com.exemplo.pagamentoservice.service.PagamentoService;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Recebe eventos de pedidos criados publicados pelo monólito.
 *
 * <p>Esta classe é a porta de entrada Kafka do pagamento-service.
 * Ela não aplica diretamente regras de pagamento; apenas recebe
 * a mensagem e encaminha o evento ao service.</p>
 */
@Component
public class PedidoCriadoConsumer {

    /**
     * Logger utilizado para acompanhar tópico, partição e offset.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PedidoCriadoConsumer.class);

    /**
     * Serviço responsável pelo processamento do evento.
     */
    private final PagamentoService pagamentoService;

    /**
     * Recebe o service por injeção de dependência.
     *
     * @param pagamentoService regras e persistência dos pagamentos
     */
    public PedidoCriadoConsumer(
            PagamentoService pagamentoService
    ) {
        this.pagamentoService = pagamentoService;
    }

    /**
     * Recebe uma mensagem do tópico pedidos-criados.
     *
     * <p>O group-id não aparece na anotação porque será obtido de
     * spring.kafka.consumer.group-id no application.yaml.</p>
     *
     * @param registro mensagem completa recebida do Kafka
     */
    @KafkaListener(
            topics = "pedidos-criados"
    )
    public void receber(
            ConsumerRecord<String, PedidoCriadoEvent> registro
    ) {

        PedidoCriadoEvent evento =
                registro.value();

        LOGGER.info(
                "Pedido recebido: topico={}, particao={}, offset={}, chave={}, eventoId={}, pedidoId={}",
                registro.topic(),
                registro.partition(),
                registro.offset(),
                registro.key(),
                evento.eventoId(),
                evento.pedidoId()
        );

        try {

            // Encaminha o evento para a camada de regras e persistência.
            Pagamento pagamento =
                    pagamentoService.registrarPedidoCriado(
                            evento
                    );

            LOGGER.info(
                    "Evento de pedido processado: pagamentoId={}, pedidoId={}, status={}",
                    pagamento.getId(),
                    pagamento.getPedidoId(),
                    pagamento.getStatus()
            );

        } catch (RuntimeException exception) {

            /*
             * Registra o erro e relança a exceção.
             *
             * Relançar é importante: se apenas ocultássemos a falha,
             * o Kafka poderia considerar a mensagem processada.
             */
            LOGGER.error(
                    "Falha ao processar evento: topico={}, particao={}, offset={}, pedidoId={}",
                    registro.topic(),
                    registro.partition(),
                    registro.offset(),
                    evento == null ? null : evento.pedidoId(),
                    exception
            );

            throw exception;
        }
    }
}