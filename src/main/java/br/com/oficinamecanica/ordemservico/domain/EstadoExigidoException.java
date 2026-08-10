package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import java.util.UUID;

public class EstadoExigidoException extends ConflitoDeEstadoException {

    public EstadoExigidoException(UUID ordemServicoId, StatusOrdemServico atual, StatusOrdemServico exigido) {
        super("TRANSICAO_INVALIDA", "A Ordem de Servico " + ordemServicoId + " esta em " + atual
                + " e esta operacao exige que ela esteja em " + exigido);
    }
}
