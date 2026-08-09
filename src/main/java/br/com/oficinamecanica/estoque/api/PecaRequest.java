package br.com.oficinamecanica.estoque.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PecaRequest(
        @NotBlank @Size(max = 120) String nome,
        @Size(min = 1, max = 20) String unidadeMedida,
        @NotNull @Valid DinheiroRequest preco,
        @NotNull @Min(0) @Max(1_000_000) Integer estoqueMinimo) {
}
