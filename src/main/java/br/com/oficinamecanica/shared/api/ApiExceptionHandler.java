package br.com.oficinamecanica.shared.api;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String CODIGO_REQUISICAO_INVALIDA = "REQUISICAO_INVALIDA";

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroResponse> dadosInvalidos(DadosInvalidosException excecao) {
        return resposta(HttpStatus.BAD_REQUEST, excecao.codigo(), excecao.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException excecao) {
        return resposta(HttpStatus.NOT_FOUND, excecao.codigo(), excecao.getMessage());
    }

    @ExceptionHandler(ConflitoDeEstadoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoDeEstadoException excecao) {
        return resposta(HttpStatus.CONFLICT, excecao.codigo(), excecao.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(MethodArgumentNotValidException excecao) {
        String mensagem = excecao.getBindingResult().getFieldErrors().stream()
                .map(this::descreve)
                .collect(Collectors.joining("; "));
        return resposta(HttpStatus.BAD_REQUEST, CODIGO_REQUISICAO_INVALIDA, mensagem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return resposta(HttpStatus.BAD_REQUEST, CODIGO_REQUISICAO_INVALIDA, "Corpo da requisicao ausente ou malformado");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tipoDeConteudoNaoSuportado(HttpMediaTypeNotSupportedException excecao) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, CODIGO_REQUISICAO_INVALIDA,
                "Content-Type ausente ou nao suportado; use application/json");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroComTipoErrado(MethodArgumentTypeMismatchException excecao) {
        return resposta(HttpStatus.BAD_REQUEST, CODIGO_REQUISICAO_INVALIDA,
                excecao.getName() + " tem formato invalido");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> violacaoDeUnicidade(DataIntegrityViolationException excecao) {
        return resposta(HttpStatus.CONFLICT, "CONFLITO_DE_ESTADO",
                "O registro conflita com outro ja existente");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException excecao) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, CODIGO_REQUISICAO_INVALIDA,
                "Metodo " + excecao.getMethod() + " nao suportado nesta rota");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErroResponse> formatoDeRespostaNaoAceito(HttpMediaTypeNotAcceptableException excecao) {
        return resposta(HttpStatus.NOT_ACCEPTABLE, CODIGO_REQUISICAO_INVALIDA,
                "Esta API responde apenas application/json");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroInterno(Exception excecao) {
        log.error("Erro nao tratado ao processar a requisicao", excecao);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Erro interno ao processar a requisicao");
    }

    private String descreve(FieldError erro) {
        return erro.getField() + ": " + erro.getDefaultMessage();
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String codigo, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponse(codigo, mensagem));
    }
}
