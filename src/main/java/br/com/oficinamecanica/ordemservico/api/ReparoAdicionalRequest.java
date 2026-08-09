package br.com.oficinamecanica.ordemservico.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReparoAdicionalRequest(
        @NotBlank @Size(max = 500) String descricao,
        @Valid @Size(min = 1) List<IncluirItensRequest.ItemDeServico> itensServico,
        @Valid @Size(min = 1) List<IncluirItensRequest.ItemDePeca> itensPeca) {

    @AssertTrue(message = "informe ao menos um item de servico ou de peca")
    public boolean isPeloMenosUmItem() {
        return IncluirItensRequest.temItem(itensServico, itensPeca);
    }
}
