package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.util.UUID;

public class ItemPeca {

    private static final String CODIGO = "ITEM_PECA_INVALIDO";

    private final UUID id;
    private final UUID pecaId;
    private final int quantidade;
    private final Dinheiro precoSnapshot;
    private final int versaoOrigem;

    public ItemPeca(UUID id, UUID pecaId, int quantidade, Dinheiro precoSnapshot, int versaoOrigem) {
        if (id == null || pecaId == null) {
            throw new DadosInvalidosException(CODIGO, "Item de peca exige identidade e Peca");
        }
        if (quantidade <= 0) {
            throw new DadosInvalidosException(CODIGO, "Item de peca exige quantidade positiva");
        }
        if (precoSnapshot == null) {
            throw new DadosInvalidosException(CODIGO, "Item de peca exige o preco copiado");
        }
        if (versaoOrigem < 1) {
            throw new DadosInvalidosException(CODIGO, "Item de peca exige a Versao do orcamento que o introduziu");
        }
        this.id = id;
        this.pecaId = pecaId;
        this.quantidade = quantidade;
        this.precoSnapshot = precoSnapshot;
        this.versaoOrigem = versaoOrigem;
    }

    static ItemPeca incluir(UUID pecaId, int quantidade, Dinheiro preco, int versaoOrigem) {
        return new ItemPeca(UUID.randomUUID(), pecaId, quantidade, preco, versaoOrigem);
    }

    Dinheiro subtotal() {
        return precoSnapshot.multiplicar(quantidade);
    }

    public UUID id() {
        return id;
    }

    public UUID pecaId() {
        return pecaId;
    }

    public int quantidade() {
        return quantidade;
    }

    public Dinheiro precoSnapshot() {
        return precoSnapshot;
    }

    public int versaoOrigem() {
        return versaoOrigem;
    }
}
