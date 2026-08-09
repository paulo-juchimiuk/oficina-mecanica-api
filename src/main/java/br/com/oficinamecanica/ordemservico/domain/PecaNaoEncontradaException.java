package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;
import java.util.UUID;

public class PecaNaoEncontradaException extends RecursoNaoEncontradoException {

    public PecaNaoEncontradaException(UUID id) {
        super("PECA_NAO_ENCONTRADA", "Peca " + id + " nao encontrada no catalogo");
    }
}
