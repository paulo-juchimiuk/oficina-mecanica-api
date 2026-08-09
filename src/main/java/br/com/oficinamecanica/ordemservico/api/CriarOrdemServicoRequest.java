package br.com.oficinamecanica.ordemservico.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CriarOrdemServicoRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{11}$|^[0-9]{14}$") String documentoCliente,
        @NotNull UUID veiculoId,
        @Size(max = 1000) String relatoDoProblema) {
}
