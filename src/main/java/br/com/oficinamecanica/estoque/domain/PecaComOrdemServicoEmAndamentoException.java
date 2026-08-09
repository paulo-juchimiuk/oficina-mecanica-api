package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class PecaComOrdemServicoEmAndamentoException extends ConflitoDeEstadoException {

    public PecaComOrdemServicoEmAndamentoException() {
        super("PECA_COM_ORDEM_SERVICO_EM_ANDAMENTO",
                "Peca consta em Ordem de Servico em andamento e nao pode ser inativada");
    }
}
