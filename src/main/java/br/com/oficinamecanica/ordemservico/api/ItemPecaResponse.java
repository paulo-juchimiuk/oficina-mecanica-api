package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.domain.ItemPeca;
import java.util.UUID;

public record ItemPecaResponse(UUID pecaId, int quantidade, DinheiroResponse precoSnapshot, int versaoOrcamento) {

    static ItemPecaResponse de(ItemPeca item) {
        return new ItemPecaResponse(item.pecaId(), item.quantidade(),
                DinheiroResponse.de(item.precoSnapshot()), item.versaoOrigem());
    }
}
