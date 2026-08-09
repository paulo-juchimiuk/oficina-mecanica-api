package br.com.oficinamecanica.catalogo.api;

import br.com.oficinamecanica.catalogo.domain.Dinheiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DinheiroRequest(@NotNull BigDecimal valor, @NotBlank String moeda) {

    Dinheiro paraDominio() {
        return new Dinheiro(valor, moeda);
    }
}
