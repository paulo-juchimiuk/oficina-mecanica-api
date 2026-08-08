package br.com.oficinamecanica.cadastro.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Pattern(regexp = "^[0-9]{11}$|^[0-9]{14}$") String documento,
        @NotBlank @Email @Size(max = 120) String email,
        @Size(max = 20) String telefone) {
}
