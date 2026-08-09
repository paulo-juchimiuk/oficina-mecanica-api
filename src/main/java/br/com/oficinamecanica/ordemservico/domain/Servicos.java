package br.com.oficinamecanica.ordemservico.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface Servicos {

    Optional<Dinheiro> valorMaoDeObraDe(UUID servicoId);

    Map<UUID, String> nomesDe(Collection<UUID> servicoIds);
}
