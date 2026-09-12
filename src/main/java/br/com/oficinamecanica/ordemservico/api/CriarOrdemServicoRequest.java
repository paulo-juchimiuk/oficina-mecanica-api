package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.api.IncluirItensRequest.ItemPecaRequest;
import br.com.oficinamecanica.ordemservico.api.IncluirItensRequest.ItemServicoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CriarOrdemServicoRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{11}$|^[0-9]{14}$") String documentoCliente,
        @NotNull UUID veiculoId,
        @Size(max = 1000) String relatoDoProblema,
        @Size(min = 1) List<@Valid @NotNull ItemServicoRequest> itensServico,
        @Size(min = 1) List<@Valid @NotNull ItemPecaRequest> itensPeca) {
}
