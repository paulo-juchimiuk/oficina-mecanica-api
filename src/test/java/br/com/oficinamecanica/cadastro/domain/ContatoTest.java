package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Contato")
class ContatoTest {

    @Test
    @DisplayName("deve exigir e-mail, que e o destino do envio do Orcamento")
    void deveExigirEmail() {
        assertThatThrownBy(() -> new Contato(null, "11999990000"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    @DisplayName("deve recusar e-mail composto so de espacos")
    void deveRecusarEmailEmBranco() {
        assertThatThrownBy(() -> new Contato("   ", null)).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve aceitar telefone ausente, porque e opcional")
    void deveAceitarTelefoneAusente() {
        assertThat(new Contato("cliente@example.com", null).telefone()).isNull();
    }

    @Test
    @DisplayName("deve tratar telefone em branco como ausente")
    void deveTratarTelefoneEmBrancoComoAusente() {
        assertThat(new Contato("cliente@example.com", "  ").telefone()).isNull();
    }

    @Test
    @DisplayName("deve remover espacos em volta do e-mail e do telefone")
    void deveRemoverEspacos() {
        Contato contato = new Contato(" cliente@example.com ", " 11999990000 ");
        assertThat(contato.email()).isEqualTo("cliente@example.com");
        assertThat(contato.telefone()).isEqualTo("11999990000");
    }
}
