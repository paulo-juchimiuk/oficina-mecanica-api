package br.com.oficinamecanica.ordemservico.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface Pecas {

    Optional<Dinheiro> precoDe(UUID pecaId);

    Map<UUID, String> nomesDe(Collection<UUID> pecaIds);
}
