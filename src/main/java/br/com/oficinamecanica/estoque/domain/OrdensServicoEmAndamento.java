package br.com.oficinamecanica.estoque.domain;

import java.util.UUID;

public interface OrdensServicoEmAndamento {

    boolean existemParaPeca(UUID pecaId);
}
