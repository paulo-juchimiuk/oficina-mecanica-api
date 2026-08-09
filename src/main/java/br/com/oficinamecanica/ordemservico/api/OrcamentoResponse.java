package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import br.com.oficinamecanica.ordemservico.domain.SituacaoOrcamento;
import java.time.LocalDateTime;

public record OrcamentoResponse(
        int versao,
        int validadeDias,
        SituacaoOrcamento situacao,
        DinheiroResponse total,
        LocalDateTime dataEnvio,
        LocalDateTime dataResposta,
        String descricao) {

    static OrcamentoResponse de(Orcamento orcamento) {
        return new OrcamentoResponse(
                orcamento.versao(),
                orcamento.validadeDias(),
                orcamento.situacao(),
                DinheiroResponse.de(orcamento.total()),
                orcamento.dataEnvio(),
                orcamento.dataResposta(),
                orcamento.descricao());
    }
}
