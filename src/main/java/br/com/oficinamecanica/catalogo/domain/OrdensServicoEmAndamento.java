package br.com.oficinamecanica.catalogo.domain;

import java.util.UUID;

public interface OrdensServicoEmAndamento {

    boolean existemParaServico(UUID servicoId);
}
