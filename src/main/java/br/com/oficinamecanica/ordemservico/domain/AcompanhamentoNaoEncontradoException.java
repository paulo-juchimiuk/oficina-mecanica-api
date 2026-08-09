package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;

public class AcompanhamentoNaoEncontradoException extends RecursoNaoEncontradoException {

    public AcompanhamentoNaoEncontradoException() {
        super("ACOMPANHAMENTO_NAO_ENCONTRADO",
                "Nenhuma Ordem de Servico corresponde a este codigo de acompanhamento");
    }
}
