package br.com.oficinamecanica.ordemservico.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record IncluirItensRequest(
        @Valid @Size(min = 1) List<ItemDeServico> itensServico,
        @Valid @Size(min = 1) List<ItemDePeca> itensPeca) {

    @AssertTrue(message = "informe ao menos um item de servico ou de peca")
    public boolean isPeloMenosUmItem() {
        return temItem(itensServico, itensPeca);
    }

    static boolean temItem(List<ItemDeServico> servicos, List<ItemDePeca> pecas) {
        return naoVazia(servicos) || naoVazia(pecas);
    }

    private static boolean naoVazia(List<?> itens) {
        return itens != null && !itens.isEmpty();
    }

    public record ItemDeServico(@NotNull UUID servicoId) {
    }

    public record ItemDePeca(@NotNull UUID pecaId, @NotNull @Min(1) @Max(1000000) Integer quantidade) {
    }
}
