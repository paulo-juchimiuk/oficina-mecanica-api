package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import java.util.UUID;

public class TransicaoInvalidaException extends ConflitoDeEstadoException {

    public TransicaoInvalidaException(UUID ordemServicoId, StatusOrdemServico origem, StatusOrdemServico destino) {
        super("TRANSICAO_INVALIDA", "A Ordem de Servico " + ordemServicoId + " esta em " + origem
                + " e a maquina de estados nao aceita a transicao para " + destino);
    }
}
