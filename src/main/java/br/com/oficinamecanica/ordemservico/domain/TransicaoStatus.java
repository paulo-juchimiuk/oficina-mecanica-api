package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.time.LocalDateTime;

public record TransicaoStatus(StatusOrdemServico deStatus, StatusOrdemServico paraStatus, LocalDateTime dataHora) {

    private static final String CODIGO = "TRANSICAO_INVALIDA";

    public TransicaoStatus {
        if (paraStatus == null) {
            throw new DadosInvalidosException(CODIGO, "Transicao de status exige o status de destino");
        }
        if (dataHora == null) {
            throw new DadosInvalidosException(CODIGO, "Transicao de status exige data e hora");
        }
    }

    static TransicaoStatus abertura(StatusOrdemServico inicial) {
        return new TransicaoStatus(null, inicial, LocalDateTime.now());
    }

    static TransicaoStatus entre(StatusOrdemServico origem, StatusOrdemServico destino) {
        return new TransicaoStatus(origem, destino, LocalDateTime.now());
    }
}
