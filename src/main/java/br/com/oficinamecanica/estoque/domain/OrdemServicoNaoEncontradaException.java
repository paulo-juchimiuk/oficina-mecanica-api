package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;
import java.util.UUID;

public class OrdemServicoNaoEncontradaException extends RecursoNaoEncontradoException {

    public OrdemServicoNaoEncontradaException(UUID id) {
        super("ORDEM_SERVICO_NAO_ENCONTRADA", "Ordem de Servico " + id + " nao encontrada");
    }
}
