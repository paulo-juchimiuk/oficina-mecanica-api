package br.com.oficinamecanica.autenticacao.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Credenciais(
        @NotBlank @Size(max = 60) String login,
        @NotBlank String senha) {
}
