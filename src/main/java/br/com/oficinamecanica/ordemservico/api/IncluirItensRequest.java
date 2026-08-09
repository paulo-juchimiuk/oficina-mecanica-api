package br.com.oficinamecanica.ordemservico.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record IncluirItensRequest(
        @Valid @Size(min = 1) List<ItemDeServico> itensServico,
        @Valid @Size(min = 1) List<ItemDePeca> itensPeca) {

    public record ItemDeServico(@NotNull UUID servicoId) {
    }

    public record ItemDePeca(@NotNull UUID pecaId, @NotNull @Min(1) @Max(1000000) Integer quantidade) {
    }
}
