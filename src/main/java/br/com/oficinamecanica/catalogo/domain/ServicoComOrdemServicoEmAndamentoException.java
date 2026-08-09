package br.com.oficinamecanica.catalogo.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class ServicoComOrdemServicoEmAndamentoException extends ConflitoDeEstadoException {

    public ServicoComOrdemServicoEmAndamentoException() {
        super("SERVICO_COM_ORDEM_SERVICO_EM_ANDAMENTO",
                "Servico consta em Ordem de Servico em andamento e nao pode ser inativado");
    }
}
