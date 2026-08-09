package br.com.oficinamecanica.catalogo.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Servico")
class ServicoTest {

    private static final Dinheiro VALOR = new Dinheiro(new BigDecimal("189.90"), "BRL");

    @Test
    @DisplayName("deve nascer ativo com identidade propria")
    void deveNascerAtivo() {
        Servico servico = Servico.cadastrar("Troca de oleo", "Inclui filtro", VALOR);

        assertThat(servico.id()).isNotNull();
        assertThat(servico.ativo()).isTrue();
        assertThat(servico.nome()).isEqualTo("Troca de oleo");
        assertThat(servico.descricao()).isEqualTo("Inclui filtro");
        assertThat(servico.valorMaoDeObra()).isEqualTo(VALOR);
    }

    @Test
    @DisplayName("deve aceitar descricao ausente, que e opcional no catalogo")
    void deveAceitarDescricaoAusente() {
        assertThat(Servico.cadastrar("Alinhamento", null, VALOR).descricao()).isNull();
    }

    @Test
    @DisplayName("deve tratar descricao em branco como ausente")
    void deveTratarDescricaoEmBrancoComoAusente() {
        assertThat(Servico.cadastrar("Alinhamento", "   ", VALOR).descricao()).isNull();
    }

    @Test
    @DisplayName("deve ignorar espacos em volta do nome e da descricao")
    void deveIgnorarEspacos() {
        Servico servico = Servico.cadastrar("  Balanceamento  ", "  quatro rodas  ", VALOR);

        assertThat(servico.nome()).isEqualTo("Balanceamento");
        assertThat(servico.descricao()).isEqualTo("quatro rodas");
    }

    @Test
    @DisplayName("deve exigir nome")
    void deveExigirNome() {
        assertThatThrownBy(() -> Servico.cadastrar("   ", null, VALOR))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("nome");
    }

    @Test
    @DisplayName("deve exigir Valor de mao de obra")
    void deveExigirValorMaoDeObra() {
        assertThatThrownBy(() -> Servico.cadastrar("Troca de oleo", null, null))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("Valor de mao de obra");
    }

    @Test
    @DisplayName("deve exigir identidade ao reconstruir a partir da persistencia")
    void deveExigirIdentidade() {
        assertThatThrownBy(() -> new Servico(null, "Troca de oleo", null, VALOR, true))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("identidade");
    }

    @Test
    @DisplayName("deve alterar nome, descricao e Valor de mao de obra preservando a identidade")
    void deveAlterar() {
        Servico servico = Servico.cadastrar("Troca de oleo", "Inclui filtro", VALOR);
        UUID identidade = servico.id();
        Dinheiro novoValor = new Dinheiro(new BigDecimal("249.00"), "BRL");

        servico.alterar("Troca de oleo sintetico", null, novoValor);

        assertThat(servico.id()).isEqualTo(identidade);
        assertThat(servico.nome()).isEqualTo("Troca de oleo sintetico");
        assertThat(servico.descricao()).isNull();
        assertThat(servico.valorMaoDeObra()).isEqualTo(novoValor);
    }

    @Test
    @DisplayName("deve inativar sem perder os dados do catalogo")
    void deveInativar() {
        Servico servico = Servico.cadastrar("Troca de oleo", "Inclui filtro", VALOR);

        servico.inativar();

        assertThat(servico.ativo()).isFalse();
        assertThat(servico.nome()).isEqualTo("Troca de oleo");
    }

    @Test
    @DisplayName("deve reconstruir inativo a partir da persistencia")
    void deveReconstruirInativo() {
        Servico servico = new Servico(UUID.randomUUID(), "Troca de oleo", null, VALOR, false);

        assertThat(servico.ativo()).isFalse();
    }
}
