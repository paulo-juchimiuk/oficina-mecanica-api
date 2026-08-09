package br.com.oficinamecanica.estoque.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FaltaPecaRequest(
        @NotNull UUID pecaId,
        @NotNull @Min(1) @Max(1_000_000) Integer quantidadeFaltante) {
}
