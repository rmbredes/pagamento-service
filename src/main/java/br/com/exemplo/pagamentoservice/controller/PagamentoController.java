package br.com.exemplo.pagamentoservice.controller;

import br.com.exemplo.pagamentoservice.dto.PagamentoResponse;
import br.com.exemplo.pagamentoservice.dto.RecusarPagamentoRequest;
import br.com.exemplo.pagamentoservice.service.PagamentoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints HTTP do pagamento-service.
 *
 * Neste momento eles ainda não possuem autenticação.
 * Adicionaremos a segurança somente depois que todo o fluxo
 * funcional estiver pronto e testado.
 */
@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    /**
     * Lista todos os pagamentos registrados pelo consumidor Kafka.
     */
    @GetMapping
    public List<PagamentoResponse> listar() {
        return pagamentoService.listar()
                .stream()
                .map(PagamentoResponse::from)
                .toList();
    }

    /**
     * Consulta pelo ID interno do pagamento.
     *
     * Exemplo: GET /pagamentos/1
     */
    @GetMapping("/{pagamentoId}")
    public PagamentoResponse buscarPorId(
            @PathVariable Long pagamentoId
    ) {
        return PagamentoResponse.from(
                pagamentoService.buscarPorId(pagamentoId)
        );
    }

    /**
     * Consulta pelo ID do pedido recebido do monólito.
     *
     * Exemplo: GET /pagamentos/pedido/810
     */
    @GetMapping("/pedido/{pedidoId}")
    public PagamentoResponse buscarPorPedido(
            @PathVariable Long pedidoId
    ) {
        return PagamentoResponse.from(
                pagamentoService.buscarPorPedidoId(pedidoId)
        );
    }

    /**
     * Aprova manualmente um pagamento PENDENTE.
     */
    @PostMapping("/{pagamentoId}/aprovar")
    public PagamentoResponse aprovar(
            @PathVariable Long pagamentoId
    ) {
        return PagamentoResponse.from(
                pagamentoService.aprovar(pagamentoId)
        );
    }

    /**
     * Recusa manualmente um pagamento PENDENTE.
     */
    @PostMapping("/{pagamentoId}/recusar")
    public PagamentoResponse recusar(
            @PathVariable Long pagamentoId,
            @Valid @RequestBody RecusarPagamentoRequest request
    ) {
        return PagamentoResponse.from(
                pagamentoService.recusar(
                        pagamentoId,
                        request.motivo()
                )
        );
    }
}