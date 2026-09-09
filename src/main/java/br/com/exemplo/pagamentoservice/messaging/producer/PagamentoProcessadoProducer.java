package br.com.exemplo.pagamentoservice.messaging.producer;

import br.com.exemplo.pagamentoservice.event.PagamentoProcessadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Responsável exclusivamente por publicar eventos de pagamentos
 * processados no Kafka.
 */
@Component
public class PagamentoProcessadoProducer {

    private static final Logger log =
            LoggerFactory.getLogger(PagamentoProcessadoProducer.class);

    private static final String TOPICO = "pagamentos-processados";

    private final KafkaTemplate<String, PagamentoProcessadoEvent> kafkaTemplate;

    public PagamentoProcessadoProducer(
            KafkaTemplate<String, PagamentoProcessadoEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(PagamentoProcessadoEvent evento) {
        /*
         * Usamos o pedidoId como chave.
         *
         * Mensagens com a mesma chave tendem a ir para a mesma partição,
         * preservando a ordem dos eventos daquele pedido.
         */
        String chave = evento.pedidoId().toString();

        /*
         * O envio é assíncrono: send() devolve imediatamente um futuro.
         * O whenComplete será chamado quando o Kafka confirmar o envio
         * ou informar uma falha.
         */
        kafkaTemplate.send(TOPICO, chave, evento)
                .whenComplete((resultado, erro) -> {
                    if (erro != null) {
                        log.error(
                                "Falha ao publicar pagamento processado: " +
                                        "eventoId={}, pagamentoId={}, pedidoId={}",
                                evento.eventoId(),
                                evento.pagamentoId(),
                                evento.pedidoId(),
                                erro
                        );

                        return;
                    }

                    log.info(
                            "Pagamento processado publicado: " +
                                    "eventoId={}, pagamentoId={}, pedidoId={}, " +
                                    "status={}, topico={}, particao={}, offset={}",
                            evento.eventoId(),
                            evento.pagamentoId(),
                            evento.pedidoId(),
                            evento.status(),
                            resultado.getRecordMetadata().topic(),
                            resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset()
                    );
                });
    }
}