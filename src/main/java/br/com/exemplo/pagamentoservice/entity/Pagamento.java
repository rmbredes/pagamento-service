package br.com.exemplo.pagamentoservice.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa um pagamento persistido no banco exclusivo
 * do pagamento-service.
 *
 * <p>Essa entidade não possui relacionamento JPA com Pedido ou Cliente,
 * pois essas entidades pertencem ao banco do monólito.</p>
 */
@Entity
@Table(
        name = "pagamentos",
        uniqueConstraints = {

                /*
                 * Impede que o mesmo evento Kafka
                 * crie mais de um pagamento.
                 */
                @UniqueConstraint(
                        name = "uk_pagamentos_evento_id",
                        columnNames = "evento_id"
                ),

                /*
                 * Nesta primeira versão, cada pedido
                 * possuirá somente um pagamento.
                 */
                @UniqueConstraint(
                        name = "uk_pagamentos_pedido_id",
                        columnNames = "pedido_id"
                )
        }
)
public class Pagamento {

    /**
     * Identificador interno gerado pelo PostgreSQL.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador do evento Kafka que originou o pagamento.
     */
    @Column(
            name = "evento_id",
            nullable = false
    )
    private UUID eventoId;

    /**
     * Identificador lógico do pedido existente no monólito.
     *
     * <p>Não é uma chave estrangeira porque o pedido está
     * armazenado em outro banco.</p>
     */
    @Column(
            name = "pedido_id",
            nullable = false
    )
    private Long pedidoId;

    /**
     * Identificador lógico do cliente existente no monólito.
     */
    @Column(
            name = "cliente_id",
            nullable = false
    )
    private Long clienteId;

    /**
     * Valor recebido no evento de criação do pedido.
     *
     * <p>precision 19 representa a quantidade total de dígitos.
     * scale 2 reserva duas casas decimais.</p>
     */
    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal valor;

    /**
     * Estado atual do pagamento.
     *
     * <p>EnumType.STRING grava textos como PENDENTE e APROVADO.
     * Isso é mais legível e seguro do que armazenar posições numéricas.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private StatusPagamento status;

    /**
     * Explicação da recusa.
     *
     * <p>Permanece nula enquanto o pagamento estiver pendente
     * ou quando for aprovado.</p>
     */
    @Column(
            name = "motivo_recusa",
            length = 500
    )
    private String motivoRecusa;

    /**
     * Instante em que o pedido foi criado no monólito.
     */
    @Column(
            name = "pedido_criado_em",
            nullable = false
    )
    private Instant pedidoCriadoEm;

    /**
     * Instante em que o pagamento foi registrado neste serviço.
     */
    @Column(
            name = "criado_em",
            nullable = false
    )
    private Instant criadoEm;

    /**
     * Instante da última modificação realizada no pagamento.
     */
    @Column(
            name = "atualizado_em",
            nullable = false
    )
    private Instant atualizadoEm;

    /**
     * Instante em que o pagamento foi aprovado ou recusado.
     */
    @Column(name = "processado_em")
    private Instant processadoEm;

    /**
     * Construtor exigido pelo JPA.
     *
     * <p>O JPA utiliza reflexão para reconstruir a entidade
     * a partir dos dados encontrados no banco.</p>
     */
    public Pagamento() {
    }

    /**
     * Cria um pagamento pendente a partir dos dados recebidos.
     *
     * @param eventoId identificador do evento Kafka
     * @param pedidoId identificador do pedido
     * @param clienteId identificador do cliente
     * @param valor valor do pedido
     * @param pedidoCriadoEm instante original da criação do pedido
     */
    public Pagamento(
            UUID eventoId,
            Long pedidoId,
            Long clienteId,
            BigDecimal valor,
            Instant pedidoCriadoEm
    ) {
        // Copia os dados imutáveis recebidos do evento.
        this.eventoId = eventoId;
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.valor = valor;
        this.pedidoCriadoEm = pedidoCriadoEm;

        // Todo pagamento nasce aguardando processamento.
        this.status = StatusPagamento.PENDENTE;

        // Utiliza o mesmo instante para criação e primeira atualização.
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    /**
     * Aprova um pagamento que ainda esteja pendente.
     */
    public void aprovar() {

        // Impede a alteração de um pagamento já finalizado.
        validarPendente();

        // Registra o novo estado.
        this.status = StatusPagamento.APROVADO;

        // Uma aprovação não possui motivo de recusa.
        this.motivoRecusa = null;

        // Registra quando o processamento aconteceu.
        Instant agora = Instant.now();
        this.processadoEm = agora;
        this.atualizadoEm = agora;
    }

    /**
     * Recusa um pagamento pendente.
     *
     * @param motivo explicação da recusa
     */
    public void recusar(
            String motivo
    ) {

        // O motivo faz parte do registro da recusa.
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "O motivo da recusa deve ser informado"
            );
        }

        // Impede a alteração de um pagamento já finalizado.
        validarPendente();

        // Registra o estado e remove espaços externos do motivo.
        this.status = StatusPagamento.RECUSADO;
        this.motivoRecusa = motivo.trim();

        // Registra quando o processamento aconteceu.
        Instant agora = Instant.now();
        this.processadoEm = agora;
        this.atualizadoEm = agora;
    }

    /**
     * Confirma que o pagamento ainda pode ser processado.
     */
    private void validarPendente() {

        if (this.status != StatusPagamento.PENDENTE) {
            throw new IllegalStateException(
                    "Somente pagamentos pendentes podem ser processados"
            );
        }
    }

    public Long getId() {
        return id;
    }

    public UUID getEventoId() {
        return eventoId;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public String getMotivoRecusa() {
        return motivoRecusa;
    }

    public Instant getPedidoCriadoEm() {
        return pedidoCriadoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Instant getProcessadoEm() {
        return processadoEm;
    }
}