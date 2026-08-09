package br.com.oficinamecanica.ordemservico.domain;

import java.util.Optional;
import java.util.UUID;

public interface Clientes {

    Optional<UUID> identidadePorDocumento(String documento);

    Optional<String> emailDe(UUID clienteId);
}
