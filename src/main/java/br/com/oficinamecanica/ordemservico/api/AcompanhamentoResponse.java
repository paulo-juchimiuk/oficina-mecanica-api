package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.application.Acompanhamento;
import br.com.oficinamecanica.ordemservico.domain.DescricaoDoVeiculo;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import java.util.List;

public record AcompanhamentoResponse(
        StatusOrdemServico status,
        String veiculo,
        OrcamentoResponse orcamentoMaisRecente,
        List<TransicaoStatusResponse> transicoesStatus) {

    static AcompanhamentoResponse de(Acompanhamento acompanhamento) {
        List<ItemOrcamentoResponse> itens = acompanhamento.itensDoOrcamento().stream()
                .map(ItemOrcamentoResponse::de)
                .toList();
        return new AcompanhamentoResponse(
                acompanhamento.status(),
                descreve(acompanhamento.veiculo()),
                acompanhamento.orcamentoMaisRecente()
                        .map(orcamento -> OrcamentoResponse.comItens(orcamento, itens))
                        .orElse(null),
                acompanhamento.transicoes().stream().map(TransicaoStatusResponse::de).toList());
    }

    private static String descreve(DescricaoDoVeiculo veiculo) {
        return veiculo.marca() + " " + veiculo.modelo() + " " + veiculo.placa();
    }
}
