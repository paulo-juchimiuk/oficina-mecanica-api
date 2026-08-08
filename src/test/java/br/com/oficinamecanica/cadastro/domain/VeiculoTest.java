package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Veiculo")
class VeiculoTest {

    private static final Placa PLACA = new Placa("ABC1234");
    private static final UUID PROPRIETARIO = UUID.randomUUID();

    @Test
    @DisplayName("deve nascer ativo e com identidade propria, referenciando o Cliente por ID")
    void deveNascerAtivoComIdentidade() {
        Veiculo veiculo = Veiculo.cadastrar(PLACA, "Fiat", "Uno", 2015, PROPRIETARIO);
        assertThat(veiculo.id()).isNotNull();
        assertThat(veiculo.ativo()).isTrue();
        assertThat(veiculo.clienteId()).isEqualTo(PROPRIETARIO);
    }

    @Test
    @DisplayName("deve exigir Placa")
    void deveExigirPlaca() {
        assertThatThrownBy(() -> Veiculo.cadastrar(null, "Fiat", "Uno", 2015, PROPRIETARIO))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("Placa");
    }

    @Test
    @DisplayName("deve exigir marca")
    void deveExigirMarca() {
        assertThatThrownBy(() -> Veiculo.cadastrar(PLACA, " ", "Uno", 2015, PROPRIETARIO))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("marca");
    }

    @Test
    @DisplayName("deve exigir modelo")
    void deveExigirModelo() {
        assertThatThrownBy(() -> Veiculo.cadastrar(PLACA, "Fiat", null, 2015, PROPRIETARIO))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("modelo");
    }

    @Test
    @DisplayName("deve exigir o Cliente proprietario")
    void deveExigirProprietario() {
        assertThatThrownBy(() -> Veiculo.cadastrar(PLACA, "Fiat", "Uno", 2015, null))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("proprietario");
    }

    @Test
    @DisplayName("deve exigir identidade ao reconstruir da persistencia")
    void deveExigirIdentidade() {
        assertThatThrownBy(() -> new Veiculo(null, PLACA, "Fiat", "Uno", 2015, PROPRIETARIO, true))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("identidade");
    }

    @Test
    @DisplayName("deve aceitar troca de proprietario, que e legitima quando o carro e vendido")
    void deveAceitarTrocaDeProprietario() {
        Veiculo veiculo = Veiculo.cadastrar(PLACA, "Fiat", "Uno", 2015, PROPRIETARIO);
        UUID novoProprietario = UUID.randomUUID();

        veiculo.alterar(PLACA, "Fiat", "Uno", 2015, novoProprietario);

        assertThat(veiculo.clienteId()).isEqualTo(novoProprietario);
    }

    @Test
    @DisplayName("deve aceitar troca de Placa preservando a identidade do agregado")
    void deveAceitarTrocaDePlaca() {
        Veiculo veiculo = Veiculo.cadastrar(PLACA, "Fiat", "Uno", 2015, PROPRIETARIO);
        UUID identidade = veiculo.id();

        veiculo.alterar(new Placa("ABC1D23"), "Fiat", "Uno", 2015, PROPRIETARIO);

        assertThat(veiculo.id()).isEqualTo(identidade);
        assertThat(veiculo.placa()).isEqualTo(new Placa("ABC1D23"));
    }

    @Test
    @DisplayName("deve remover espacos em volta de marca e modelo")
    void deveRemoverEspacos() {
        Veiculo veiculo = Veiculo.cadastrar(PLACA, " Fiat ", " Uno ", 2015, PROPRIETARIO);
        assertThat(veiculo.marca()).isEqualTo("Fiat");
        assertThat(veiculo.modelo()).isEqualTo("Uno");
    }

    @Test
    @DisplayName("deve inativar sem apagar o registro, que e a remocao logica")
    void deveInativar() {
        Veiculo veiculo = Veiculo.cadastrar(PLACA, "Fiat", "Uno", 2015, PROPRIETARIO);
        veiculo.inativar();
        assertThat(veiculo.ativo()).isFalse();
    }
}
