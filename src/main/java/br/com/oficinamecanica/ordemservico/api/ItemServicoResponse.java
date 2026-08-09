package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.domain.ItemServico;
import java.util.UUID;

public record ItemServicoResponse(UUID servicoId, DinheiroResponse valorMaoDeObraSnapshot, int versaoOrcamento) {

    static ItemServicoResponse de(ItemServico item) {
        return new ItemServicoResponse(item.servicoId(),
                DinheiroResponse.de(item.valorMaoDeObraSnapshot()), item.versaoOrigem());
    }
}
