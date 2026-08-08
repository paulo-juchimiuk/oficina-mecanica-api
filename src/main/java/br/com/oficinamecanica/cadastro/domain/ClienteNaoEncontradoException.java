package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;
import java.util.UUID;

public class ClienteNaoEncontradoException extends RecursoNaoEncontradoException {

    public ClienteNaoEncontradoException(UUID id) {
        super("CLIENTE_NAO_ENCONTRADO", "Cliente " + id + " nao encontrado");
    }
}
