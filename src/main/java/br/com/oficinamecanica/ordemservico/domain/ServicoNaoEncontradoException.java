package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;
import java.util.UUID;

public class ServicoNaoEncontradoException extends RecursoNaoEncontradoException {

    public ServicoNaoEncontradoException(UUID id) {
        super("SERVICO_NAO_ENCONTRADO", "Servico " + id + " nao encontrado no catalogo");
    }
}
