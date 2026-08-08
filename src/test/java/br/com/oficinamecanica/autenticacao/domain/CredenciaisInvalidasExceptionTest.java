package br.com.oficinamecanica.autenticacao.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CredenciaisInvalidasException")
class CredenciaisInvalidasExceptionTest {

    @Test
    @DisplayName("deve dizer apenas que login ou senha sao invalidos, sem revelar qual dos dois")
    void naoDeveRevelarQualCampoFalhou() {
        CredenciaisInvalidasException excecao = new CredenciaisInvalidasException();
        assertThat(excecao.codigo()).isEqualTo("CREDENCIAIS_INVALIDAS");
        assertThat(excecao.getMessage()).isEqualTo("Login ou senha invalidos");
    }
}
