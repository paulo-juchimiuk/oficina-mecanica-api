package br.com.oficinamecanica.cadastro.domain;

import java.util.UUID;

public interface OrdensServicoEmAndamento {

    boolean existemParaCliente(UUID clienteId);

    boolean existemParaVeiculo(UUID veiculoId);
}
