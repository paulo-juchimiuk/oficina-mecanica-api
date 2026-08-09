package br.com.oficinamecanica.ordemservico.domain;

import java.util.Optional;
import java.util.UUID;

public interface Veiculos {

    Optional<UUID> proprietarioDe(UUID veiculoId);

    Optional<DescricaoDoVeiculo> descricaoDe(UUID veiculoId);
}
