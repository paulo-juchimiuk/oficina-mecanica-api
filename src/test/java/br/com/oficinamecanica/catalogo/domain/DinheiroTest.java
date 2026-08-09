package br.com.oficinamecanica.catalogo.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Dinheiro")
class DinheiroTest {

    private static final String BRL = "BRL";

    private Dinheiro reais(String valor) {
        return new Dinheiro(new BigDecimal(valor), BRL);
    }

    @Test
    @DisplayName("deve aceitar valor com duas casas decimais")
    void deveAceitarDuasCasas() {
        assertThat(reais("189.90").valor()).isEqualByComparingTo("189.90");
    }

    @Test
    @DisplayName("deve aceitar valor zero")
    void deveAceitarZero() {
        assertThat(reais("0").valor()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("deve normalizar a escala para duas casas na construcao")
    void deveNormalizarEscala() {
        assertThat(reais("189.9").valor().scale()).isEqualTo(2);
        assertThat(reais("189").valor().toPlainString()).isEqualTo("189.00");
    }

    @Test
    @DisplayName("deve comparar por valor independente da escala informada")
    void deveCompararPorValor() {
        assertThat(reais("189.9")).isEqualTo(reais("189.90"));
        assertThat(reais("189.900")).isEqualTo(reais("189.90"));
    }

    @Test
    @DisplayName("deve recusar valor nulo")
    void deveRecusarValorNulo() {
        assertThatThrownBy(() -> new Dinheiro(null, BRL))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("obrigatorio");
    }

    @Test
    @DisplayName("deve recusar valor negativo")
    void deveRecusarNegativo() {
        assertThatThrownBy(() -> reais("-0.01"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("deve recusar mais de duas casas decimais")
    void deveRecusarTresCasas() {
        assertThatThrownBy(() -> reais("189.999"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("duas casas decimais");
    }

    @Test
    @DisplayName("deve recusar valor acima do teto que a coluna persiste")
    void deveRecusarAcimaDoTeto() {
        assertThatThrownBy(() -> reais("10000000000.00"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("teto");
    }

    @Test
    @DisplayName("deve aceitar exatamente o teto")
    void deveAceitarOTeto() {
        assertThat(reais("9999999999.99").valor()).isEqualByComparingTo("9999999999.99");
    }

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "brl", ""})
    @DisplayName("deve recusar moeda diferente de BRL")
    void deveRecusarOutraMoeda(String moeda) {
        assertThatThrownBy(() -> new Dinheiro(BigDecimal.TEN, moeda))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("BRL");
    }

    @Test
    @DisplayName("deve recusar moeda nula")
    void deveRecusarMoedaNula() {
        assertThatThrownBy(() -> new Dinheiro(BigDecimal.TEN, null))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("BRL");
    }

    @Test
    @DisplayName("deve identificar o codigo de erro do dominio")
    void deveIdentificarCodigo() {
        assertThatThrownBy(() -> reais("-1"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasFieldOrPropertyWithValue("codigo", "VALOR_MONETARIO_INVALIDO");
    }
}
