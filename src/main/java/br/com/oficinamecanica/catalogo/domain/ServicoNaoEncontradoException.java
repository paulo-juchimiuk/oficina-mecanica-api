package br.com.oficinamecanica.catalogo.domain;

import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;
import java.util.UUID;

public class ServicoNaoEncontradoException extends RecursoNaoEncontradoException {

    public ServicoNaoEncontradoException(UUID id) {
        super("SERVICO_NAO_ENCONTRADO", "Servico " + id + " nao encontrado");
    }
}
