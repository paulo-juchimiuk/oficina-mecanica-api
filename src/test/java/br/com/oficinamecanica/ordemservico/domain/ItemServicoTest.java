package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Item de servico")
class ItemServicoTest {

    private static final int PRIMEIRA_VERSAO = 1;

    private final UUID servicoId = UUID.randomUUID();
    private final Dinheiro valor = new Dinheiro(new BigDecimal("189.90"), "BRL");

    @Test
    @DisplayName("deve copiar o Valor de mao de obra como snapshot e guardar a versao que o introduziu")
    void deveCopiarOValorEAVersao() {
        ItemServico item = ItemServico.incluir(servicoId, valor, PRIMEIRA_VERSAO);

        assertThat(item.servicoId()).isEqualTo(servicoId);
        assertThat(item.valorMaoDeObraSnapshot()).isEqualTo(valor);
        assertThat(item.versaoOrigem()).isEqualTo(PRIMEIRA_VERSAO);
        assertThat(item.id()).isNotNull();
    }

    @Test
    @DisplayName("deve ter como subtotal o proprio Valor de mao de obra, porque servico nao tem quantidade")
    void deveTerSubtotalIgualAoValor() {
        assertThat(ItemServico.incluir(servicoId, valor, PRIMEIRA_VERSAO).subtotal()).isEqualTo(valor);
    }

    @Test
    @DisplayName("deve exigir identidade, Servico, valor copiado e versao de origem")
    void deveExigirCamposObrigatorios() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> new ItemServico(null, servicoId, valor, PRIMEIRA_VERSAO))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new ItemServico(id, null, valor, PRIMEIRA_VERSAO))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new ItemServico(id, servicoId, null, PRIMEIRA_VERSAO))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new ItemServico(id, servicoId, valor, 0))
                .isInstanceOf(DadosInvalidosException.class);
    }
}
