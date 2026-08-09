package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Value Object Dinheiro da Ordem de Servico")
class DinheiroTest {

    private static final String MOEDA = "BRL";

    private Dinheiro reais(String valor) {
        return new Dinheiro(new BigDecimal(valor), MOEDA);
    }

    @Test
    @DisplayName("deve nascer zerado com a moeda do MVP")
    void deveNascerZerado() {
        assertThat(Dinheiro.zero()).isEqualTo(reais("0.00"));
    }

    @Test
    @DisplayName("deve somar preservando as duas casas decimais")
    void deveSomar() {
        assertThat(reais("189.90").somar(reais("10.10"))).isEqualTo(reais("200.00"));
    }

    @Test
    @DisplayName("deve multiplicar pelo numero de unidades do Item de peca")
    void deveMultiplicarPelaQuantidade() {
        assertThat(reais("189.90").multiplicar(3)).isEqualTo(reais("569.70"));
    }

    @Test
    @DisplayName("deve comparar por valor, com a escala normalizada")
    void deveCompararPorValor() {
        assertThat(reais("10.5")).isEqualTo(reais("10.50"));
    }

    @Test
    @DisplayName("deve recusar valor nulo, moeda diferente de BRL e valor negativo")
    void deveRecusarValoresInvalidos() {
        assertThatThrownBy(() -> new Dinheiro(null, MOEDA)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new Dinheiro(BigDecimal.TEN, "USD")).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> reais("-0.01")).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar mais de duas casas decimais, porque a coluna nao guarda a terceira")
    void deveRecusarTerceiraCasaDecimal() {
        assertThatThrownBy(() -> reais("10.001")).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar valor acima do teto que a coluna persiste")
    void deveRecusarValorAcimaDoTeto() {
        assertThatThrownBy(() -> reais("10000000000.00")).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar soma que estoura o teto da coluna")
    void deveRecusarSomaQueEstouraOTeto() {
        Dinheiro quaseNoTeto = reais("9999999999.99");
        assertThatThrownBy(() -> quaseNoTeto.somar(reais("0.01"))).isInstanceOf(DadosInvalidosException.class);
    }
}
