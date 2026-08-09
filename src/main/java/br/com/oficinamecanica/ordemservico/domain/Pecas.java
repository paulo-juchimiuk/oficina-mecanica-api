package br.com.oficinamecanica.ordemservico.domain;

import java.util.Optional;
import java.util.UUID;

public interface Pecas {

    Optional<Dinheiro> precoDe(UUID pecaId);
}
