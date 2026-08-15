package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.api.IncluirItensRequest.ItemPecaRequest;
import br.com.oficinamecanica.ordemservico.api.IncluirItensRequest.ItemServicoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReparoAdicionalRequest(
        @NotBlank @Size(max = 500) String descricao,
        @Size(min = 1) List<@Valid @NotNull ItemServicoRequest> itensServico,
        @Size(min = 1) List<@Valid @NotNull ItemPecaRequest> itensPeca) {

    @AssertTrue(message = "informe ao menos um item de servico ou de peca")
    public boolean isPeloMenosUmItem() {
        return IncluirItensRequest.temItem(itensServico, itensPeca);
    }
}
