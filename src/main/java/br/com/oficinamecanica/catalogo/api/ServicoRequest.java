package br.com.oficinamecanica.catalogo.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServicoRequest(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 500) String descricao,
        @NotNull @Valid DinheiroRequest valorMaoDeObra) {
}
