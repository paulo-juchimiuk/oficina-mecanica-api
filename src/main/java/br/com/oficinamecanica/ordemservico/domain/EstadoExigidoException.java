package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class EstadoExigidoException extends ConflitoDeEstadoException {

    public EstadoExigidoException(StatusOrdemServico atual, StatusOrdemServico exigido) {
        super("TRANSICAO_INVALIDA", "A Ordem de Servico esta em " + atual
                + " e esta operacao exige que ela esteja em " + exigido);
    }
}
