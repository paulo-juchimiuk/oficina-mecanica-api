package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.DescricaoDoVeiculo;
import br.com.oficinamecanica.ordemservico.domain.ItemOrcamento;
import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import br.com.oficinamecanica.ordemservico.domain.TransicaoStatus;
import java.util.List;
import java.util.Optional;

public record Acompanhamento(
        StatusOrdemServico status,
        DescricaoDoVeiculo veiculo,
        Optional<Orcamento> orcamentoMaisRecente,
        List<ItemOrcamento> itensDoOrcamento,
        List<TransicaoStatus> transicoes) {
}
