package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Cliente")
class ClienteTest {

    private static final Documento DOCUMENTO = new Documento("10433218100");
    private static final Contato CONTATO = new Contato("cliente@example.com", "11999990000");

    @Test
    @DisplayName("deve nascer ativo e com identidade propria")
    void deveNascerAtivoComIdentidade() {
        Cliente cliente = Cliente.cadastrar("Ana Beatriz Souza", DOCUMENTO, CONTATO);
        assertThat(cliente.id()).isNotNull();
        assertThat(cliente.ativo()).isTrue();
        assertThat(cliente.nome()).isEqualTo("Ana Beatriz Souza");
    }

    @Test
    @DisplayName("deve remover espacos em volta do nome")
    void deveRemoverEspacosDoNome() {
        assertThat(Cliente.cadastrar("  Ana  ", DOCUMENTO, CONTATO).nome()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("deve exigir nome preenchido")
    void deveExigirNome() {
        assertThatThrownBy(() -> Cliente.cadastrar("   ", DOCUMENTO, CONTATO))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("nome");
    }

    @Test
    @DisplayName("deve exigir Documento")
    void deveExigirDocumento() {
        assertThatThrownBy(() -> Cliente.cadastrar("Ana", null, CONTATO))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("Documento");
    }

    @Test
    @DisplayName("deve exigir Contato")
    void deveExigirContato() {
        assertThatThrownBy(() -> Cliente.cadastrar("Ana", DOCUMENTO, null))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("Contato");
    }

    @Test
    @DisplayName("deve exigir identidade ao reconstruir da persistencia")
    void deveExigirIdentidade() {
        assertThatThrownBy(() -> new Cliente(null, "Ana", DOCUMENTO, CONTATO, true))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("identidade");
    }

    @Test
    @DisplayName("deve alterar nome, Documento e Contato preservando a identidade")
    void deveAlterarPreservandoIdentidade() {
        Cliente cliente = Cliente.cadastrar("Ana", DOCUMENTO, CONTATO);
        UUID identidade = cliente.id();
        Documento novoDocumento = new Documento("96001338914");
        Contato novoContato = new Contato("nova@example.com", null);

        cliente.alterar("Ana Beatriz", novoDocumento, novoContato);

        assertThat(cliente.id()).isEqualTo(identidade);
        assertThat(cliente.nome()).isEqualTo("Ana Beatriz");
        assertThat(cliente.documento()).isEqualTo(novoDocumento);
        assertThat(cliente.contato()).isEqualTo(novoContato);
    }

    @Test
    @DisplayName("deve inativar sem apagar o registro, que e a remocao logica")
    void deveInativar() {
        Cliente cliente = Cliente.cadastrar("Ana", DOCUMENTO, CONTATO);
        cliente.inativar();
        assertThat(cliente.ativo()).isFalse();
        assertThat(cliente.id()).isNotNull();
    }
}
