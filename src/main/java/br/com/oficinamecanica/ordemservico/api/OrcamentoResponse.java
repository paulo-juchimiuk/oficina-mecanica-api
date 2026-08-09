package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import br.com.oficinamecanica.ordemservico.domain.SituacaoOrcamento;
import java.time.LocalDateTime;
import java.util.List;

public record OrcamentoResponse(
        int versao,
        int validadeDias,
        SituacaoOrcamento situacao,
        DinheiroResponse total,
        LocalDateTime dataEnvio,
        LocalDateTime dataResposta,
        String descricao,
        List<ItemOrcamentoResponse> itens) {

    static OrcamentoResponse de(Orcamento orcamento) {
        return comItens(orcamento, null);
    }

    static OrcamentoResponse comItens(Orcamento orcamento, List<ItemOrcamentoResponse> itens) {
        return new OrcamentoResponse(
                orcamento.versao(),
                orcamento.validadeDias(),
                orcamento.situacao(),
                DinheiroResponse.de(orcamento.total()),
                orcamento.dataEnvio(),
                orcamento.dataResposta(),
                orcamento.descricao(),
                itens);
    }
}
