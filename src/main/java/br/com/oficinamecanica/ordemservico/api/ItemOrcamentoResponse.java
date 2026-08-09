package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.domain.ItemOrcamento;
import br.com.oficinamecanica.ordemservico.domain.TipoItemOrcamento;

public record ItemOrcamentoResponse(
        TipoItemOrcamento tipo,
        String nome,
        Integer quantidade,
        DinheiroResponse valor) {

    static ItemOrcamentoResponse de(ItemOrcamento item) {
        return new ItemOrcamentoResponse(item.tipo(), item.nome(), item.quantidade(),
                DinheiroResponse.de(item.valor()));
    }
}
