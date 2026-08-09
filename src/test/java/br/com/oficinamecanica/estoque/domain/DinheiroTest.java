package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Value Object Dinheiro do Estoque")
class DinheiroTest {

    @Test
    @DisplayName("deve normalizar o valor para duas casas decimais")
    void deveNormalizarParaDuasCasas() {
        assertThat(new Dinheiro(new BigDecimal("35.9"), "BRL").valor()).isEqualByComparingTo("35.90");
    }

    @Test
    @DisplayName("deve aceitar valor zero, que e preco valido de brinde")
    void deveAceitarValorZero() {
        assertThat(new Dinheiro(BigDecimal.ZERO, "BRL").valor()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("deve recusar valor ausente")
    void deveRecusarValorAusente() {
        assertThatThrownBy(() -> new Dinheiro(null, "BRL"))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar valor negativo")
    void deveRecusarValorNegativo() {
        assertThatThrownBy(() -> new Dinheiro(new BigDecimal("-0.01"), "BRL"))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar mais de duas casas decimais")
    void deveRecusarTresCasas() {
        assertThatThrownBy(() -> new Dinheiro(new BigDecimal("35.901"), "BRL"))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar valor acima do teto que a coluna persiste")
    void deveRecusarAcimaDoTeto() {
        assertThatThrownBy(() -> new Dinheiro(new BigDecimal("10000000000.00"), "BRL"))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USD", "brl", ""})
    @DisplayName("deve recusar moeda diferente de BRL")
    void deveRecusarOutraMoeda(String moeda) {
        assertThatThrownBy(() -> new Dinheiro(new BigDecimal("35.90"), moeda))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve comparar por valor, e nao por identidade")
    void deveCompararPorValor() {
        assertThat(new Dinheiro(new BigDecimal("35.90"), "BRL"))
                .isEqualTo(new Dinheiro(new BigDecimal("35.9"), "BRL"));
    }
}
