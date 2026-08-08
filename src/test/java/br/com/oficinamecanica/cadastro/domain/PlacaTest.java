package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Placa")
class PlacaTest {

    @ParameterizedTest
    @ValueSource(strings = {"ABC1234", "XYZ9876"})
    @DisplayName("deve aceitar o formato antigo AAA9999")
    void deveAceitarFormatoAntigo(String placa) {
        assertThat(new Placa(placa).valor()).isEqualTo(placa);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC1D23", "RTA2E19"})
    @DisplayName("deve aceitar o formato Mercosul AAA9A99")
    void deveAceitarFormatoMercosul(String placa) {
        assertThat(new Placa(placa).valor()).isEqualTo(placa);
    }

    @Test
    @DisplayName("deve normalizar para caixa alta na construcao")
    void deveNormalizarParaCaixaAlta() {
        assertThat(new Placa("abc1d23").valor()).isEqualTo("ABC1D23");
    }

    @Test
    @DisplayName("deve ignorar espacos em volta")
    void deveIgnorarEspacos() {
        assertThat(new Placa("  ABC1234 ").valor()).isEqualTo("ABC1234");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC-1234", "AB1234", "ABCD123", "ABC12E4", "1234ABC", ""})
    @DisplayName("deve recusar formato fora dos dois padroes, hifen inclusive")
    void deveRecusarFormatoInvalido(String placa) {
        assertThatThrownBy(() -> new Placa(placa))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("AAA9999");
    }

    @Test
    @DisplayName("deve recusar Placa nula")
    void deveRecusarNula() {
        assertThatThrownBy(() -> new Placa(null))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("obrigatoria");
    }

    @Test
    @DisplayName("deve comparar por valor depois de normalizada")
    void deveCompararPorValor() {
        assertThat(new Placa("abc1234")).isEqualTo(new Placa("ABC1234"));
    }
}
