package br.com.oficinamecanica.shared.api;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String CODIGO_REQUISICAO_INVALIDA = "REQUISICAO_INVALIDA";

    private static final String MENSAGEM_TIPO_DE_CONTEUDO = "Content-Type ausente ou nao suportado; use application/json";

    private static final String CURINGA = "*";

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

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ErroResponse> parametroComEscapeInvalido(InvalidParameterException excecao) {
        return resposta(HttpStatus.BAD_REQUEST, CODIGO_REQUISICAO_INVALIDA,
                "Escape percentual invalido na query string ou no corpo; use %XX com digitos hexadecimais");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tipoDeConteudoNaoSuportado(HttpMediaTypeNotSupportedException excecao) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, CODIGO_REQUISICAO_INVALIDA, MENSAGEM_TIPO_DE_CONTEUDO);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> tipoDeConteudoComCuringa(IllegalArgumentException excecao,
                                                                HttpServletRequest requisicao) {
        if (!temCuringa(requisicao.getContentType())) {
            return erroInterno(excecao);
        }
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, CODIGO_REQUISICAO_INVALIDA, MENSAGEM_TIPO_DE_CONTEUDO);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroComTipoErrado(MethodArgumentTypeMismatchException excecao) {
        return resposta(HttpStatus.BAD_REQUEST, CODIGO_REQUISICAO_INVALIDA,
                excecao.getName() + " tem formato invalido");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> violacaoDeIntegridadeNoFlush(ConstraintViolationException excecao) {
        return conflitoProtegidoPeloBanco();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> violacaoDeIntegridade(DataIntegrityViolationException excecao) {
        return conflitoProtegidoPeloBanco();
    }

    private ResponseEntity<ErroResponse> conflitoProtegidoPeloBanco() {
        return resposta(HttpStatus.CONFLICT, "CONFLITO_DE_ESTADO",
                "A operacao viola uma regra de dominio protegida pelo banco");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaNaoEncontrada(NoResourceFoundException excecao) {
        return resposta(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "Rota nao encontrada nesta API");
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

    private boolean temCuringa(String tipoDeConteudo) {
        return tipoDeConteudo != null && tipoDeConteudo.contains(CURINGA);
    }

    private String descreve(FieldError erro) {
        return erro.getField() + ": " + erro.getDefaultMessage();
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String codigo, String mensagem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErroResponse(codigo, mensagem));
    }
}
