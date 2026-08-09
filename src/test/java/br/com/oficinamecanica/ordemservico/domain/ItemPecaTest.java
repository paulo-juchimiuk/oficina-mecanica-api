package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Item de peca")
class ItemPecaTest {

    private static final int PRIMEIRA_VERSAO = 1;
    private static final int QUANTIDADE = 3;

    private final UUID pecaId = UUID.randomUUID();
    private final Dinheiro preco = new Dinheiro(new BigDecimal("189.90"), "BRL");

    @Test
    @DisplayName("deve copiar o preco como snapshot e guardar a versao que o introduziu")
    void deveCopiarOPrecoEAVersao() {
        ItemPeca item = ItemPeca.incluir(pecaId, QUANTIDADE, preco, PRIMEIRA_VERSAO);

        assertThat(item.pecaId()).isEqualTo(pecaId);
        assertThat(item.quantidade()).isEqualTo(QUANTIDADE);
        assertThat(item.precoSnapshot()).isEqualTo(preco);
        assertThat(item.versaoOrigem()).isEqualTo(PRIMEIRA_VERSAO);
    }

    @Test
    @DisplayName("deve multiplicar o preco pela quantidade no subtotal")
    void deveMultiplicarPelaQuantidade() {
        assertThat(ItemPeca.incluir(pecaId, QUANTIDADE, preco, PRIMEIRA_VERSAO).subtotal())
                .isEqualTo(new Dinheiro(new BigDecimal("569.70"), "BRL"));
    }

    @Test
    @DisplayName("deve exigir identidade, Peca, quantidade positiva, preco copiado e versao de origem")
    void deveExigirCamposObrigatorios() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> new ItemPeca(null, pecaId, QUANTIDADE, preco, PRIMEIRA_VERSAO))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new ItemPeca(id, null, QUANTIDADE, preco, PRIMEIRA_VERSAO))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new ItemPeca(id, pecaId, 0, preco, PRIMEIRA_VERSAO))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new ItemPeca(id, pecaId, QUANTIDADE, null, PRIMEIRA_VERSAO))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new ItemPeca(id, pecaId, QUANTIDADE, preco, 0))
                .isInstanceOf(DadosInvalidosException.class);
    }
}
