package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class VeiculoComOrdemServicoEmAndamentoException extends ConflitoDeEstadoException {

    public VeiculoComOrdemServicoEmAndamentoException() {
        super("VEICULO_COM_ORDEM_SERVICO_EM_ANDAMENTO",
                "Veiculo possui Ordem de Servico em andamento e nao pode ser inativado");
    }
}
