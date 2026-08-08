package br.com.oficinamecanica.cadastro.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record VeiculoRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}[0-9]{4}$|^[A-Za-z]{3}[0-9][A-Za-z][0-9]{2}$") String placa,
        @NotBlank @Size(max = 60) String marca,
        @NotBlank @Size(max = 60) String modelo,
        @NotNull @Min(1950) @Max(2100) Integer ano,
        @NotNull UUID clienteId) {
}
