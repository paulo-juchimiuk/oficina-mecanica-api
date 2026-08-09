package br.com.oficinamecanica.ordemservico.domain;

import java.util.Optional;
import java.util.UUID;

public interface Servicos {

    Optional<Dinheiro> valorMaoDeObraDe(UUID servicoId);
}
