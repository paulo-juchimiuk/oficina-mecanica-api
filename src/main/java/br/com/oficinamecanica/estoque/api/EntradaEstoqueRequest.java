package br.com.oficinamecanica.estoque.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EntradaEstoqueRequest(@NotNull @Min(1) @Max(1_000_000) Integer quantidade) {
}
