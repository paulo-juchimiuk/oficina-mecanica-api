package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class ClienteComOrdemServicoEmAndamentoException extends ConflitoDeEstadoException {

    public ClienteComOrdemServicoEmAndamentoException() {
        super("CLIENTE_COM_ORDEM_SERVICO_EM_ANDAMENTO",
                "Cliente possui Ordem de Servico em andamento e nao pode ser inativado");
    }
}
