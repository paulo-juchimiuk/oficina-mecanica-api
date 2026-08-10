package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class TransicaoInvalidaException extends ConflitoDeEstadoException {

    public TransicaoInvalidaException(StatusOrdemServico origem, StatusOrdemServico destino) {
        super("TRANSICAO_INVALIDA", "A Ordem de Servico esta em " + origem
                + " e a maquina de estados nao aceita a transicao para " + destino);
    }
}
