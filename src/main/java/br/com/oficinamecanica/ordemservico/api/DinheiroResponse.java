package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.domain.Dinheiro;
import java.math.BigDecimal;

public record DinheiroResponse(BigDecimal valor, String moeda) {

    static DinheiroResponse de(Dinheiro dinheiro) {
        return new DinheiroResponse(dinheiro.valor(), dinheiro.moeda());
    }
}
