package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import br.com.oficinamecanica.ordemservico.domain.TransicaoStatus;
import java.time.LocalDateTime;

public record TransicaoStatusResponse(
        StatusOrdemServico deStatus,
        StatusOrdemServico paraStatus,
        LocalDateTime dataHora) {

    static TransicaoStatusResponse de(TransicaoStatus transicao) {
        return new TransicaoStatusResponse(transicao.deStatus(), transicao.paraStatus(), transicao.dataHora());
    }
}
