package br.com.oficinamecanica.estoque.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReservasRequest(@NotEmpty List<@Valid @NotNull ReservaAlvo> reservas) {

    public record ReservaAlvo(@NotNull UUID reservaId) {
    }

    List<UUID> identificadores() {
        return reservas.stream().map(ReservaAlvo::reservaId).toList();
    }
}
