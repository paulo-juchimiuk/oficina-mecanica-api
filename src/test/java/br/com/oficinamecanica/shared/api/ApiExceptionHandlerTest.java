package br.com.oficinamecanica.shared.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tratamento dos erros que nao nascem no dominio")
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    @DisplayName("deve responder 409 quando a unicidade do banco derruba a gravacao, que e o que a corrida perdida produz")
    void deveResponder409NaViolacaoDeUnicidade() {
        ResponseEntity<ErroResponse> resposta =
                handler.violacaoDeUnicidade(new DataIntegrityViolationException("duplicate key"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().codigo()).isEqualTo("CONFLITO_DE_ESTADO");
    }

    @Test
    @DisplayName("deve responder 415 quando o Content-Type nao e suportado, e nao 401 como se fosse falta de token")
    void deveResponder415NoContentTypeNaoSuportado() {
        ResponseEntity<ErroResponse> resposta =
                handler.tipoDeConteudoNaoSuportado(new HttpMediaTypeNotSupportedException("text/plain"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(resposta.getBody().codigo()).isEqualTo("REQUISICAO_INVALIDA");
    }

    @Test
    @DisplayName("deve responder 400 quando o parametro nao converte para o tipo esperado")
    void deveResponder400NoParametroComTipoErrado() {
        MethodArgumentTypeMismatchException excecao =
                new MethodArgumentTypeMismatchException("nao-e-uuid", java.util.UUID.class, "id", null, null);

        ResponseEntity<ErroResponse> resposta = handler.parametroComTipoErrado(excecao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().codigo()).isEqualTo("REQUISICAO_INVALIDA");
        assertThat(resposta.getBody().mensagem()).contains("id");
    }
}
