package br.com.exemplo.pagamentoservice.exception;

import br.com.exemplo.pagamentoservice.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Centraliza o tratamento das exceções lançadas pelos controllers.
 *
 * Sem esta classe, cada controller precisaria montar suas próprias
 * respostas de erro, causando repetição de código.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Pagamento procurado pelo ID ou pelo pedido não foi encontrado.
     *
     * HTTP 404 significa que o recurso solicitado não existe.
     */
    @ExceptionHandler(PagamentoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarPagamentoNaoEncontrado(
            PagamentoNaoEncontradoException exception,
            HttpServletRequest request
    ) {
        return criarResposta(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * Trata tentativas de executar uma operação incompatível
     * com o estado atual do pagamento.
     *
     * Exemplo: tentar aprovar um pagamento que já foi recusado.
     *
     * HTTP 409 indica um conflito com o estado atual do recurso.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErroResponse> tratarEstadoInvalido(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        return criarResposta(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * Trata argumentos que possuem um valor inválido para o negócio.
     *
     * Exemplo: motivo de recusa vazio validado dentro da entidade.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> tratarArgumentoInvalido(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return criarResposta(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * Trata erros das anotações de validação.
     *
     * No RecusarPagamentoRequest, por exemplo, o @NotBlank impede
     * que o motivo seja nulo, vazio ou composto somente por espaços.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String mensagem = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .orElse("Dados da requisição inválidos");

        return criarResposta(
                HttpStatus.BAD_REQUEST,
                mensagem,
                request.getRequestURI()
        );
    }

    /**
     * Trata JSON ausente ou que não pôde ser interpretado.
     *
     * Exemplos:
     * - JSON com vírgula sobrando;
     * - aspas não fechadas;
     * - formato incompatível com o DTO.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return criarResposta(
                HttpStatus.BAD_REQUEST,
                "O corpo da requisição está ausente ou possui JSON inválido",
                request.getRequestURI()
        );
    }

    /**
     * Método auxiliar que cria todas as respostas no mesmo formato.
     */
    private ResponseEntity<ErroResponse> criarResposta(
            HttpStatus status,
            String mensagem,
            String caminho
    ) {
        ErroResponse erro = new ErroResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                caminho
        );

        return ResponseEntity
                .status(status)
                .body(erro);
    }
}