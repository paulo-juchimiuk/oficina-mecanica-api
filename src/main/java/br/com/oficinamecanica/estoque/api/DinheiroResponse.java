package br.com.oficinamecanica.estoque.api;

import br.com.oficinamecanica.estoque.domain.Dinheiro;
import java.math.BigDecimal;

public record DinheiroResponse(BigDecimal valor, String moeda) {

    static DinheiroResponse de(Dinheiro dinheiro) {
        return new DinheiroResponse(dinheiro.valor(), dinheiro.moeda());
    }
}
