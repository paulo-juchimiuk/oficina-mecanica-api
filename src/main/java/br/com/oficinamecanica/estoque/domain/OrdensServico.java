package br.com.oficinamecanica.estoque.domain;

import java.util.UUID;

public interface OrdensServico {

    boolean existe(UUID ordemServicoId);

    boolean estaEmExecucao(UUID ordemServicoId);

    boolean aceitaDevolucaoDePecas(UUID ordemServicoId);
}
