package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;

public class ClienteNaoEncontradoException extends RecursoNaoEncontradoException {

    public ClienteNaoEncontradoException(String documento) {
        super("CLIENTE_NAO_ENCONTRADO", "Nenhum Cliente ativo com o Documento " + documento);
    }
}
