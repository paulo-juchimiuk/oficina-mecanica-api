package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrdemServicoResponse(
        UUID id,
        StatusOrdemServico status,
        UUID clienteId,
        UUID veiculoId,
        LocalDateTime criadaEm,
        String codigoAcompanhamento,
        String relatoDoProblema,
        List<TransicaoStatusResponse> transicoesStatus) {

    static OrdemServicoResponse de(OrdemServico ordemServico) {
        return new OrdemServicoResponse(
                ordemServico.id(),
                ordemServico.status(),
                ordemServico.clienteId(),
                ordemServico.veiculoId(),
                ordemServico.criadaEm(),
                ordemServico.codigoAcompanhamento().valor(),
                ordemServico.relatoDoProblema(),
                ordemServico.transicoes().stream().map(TransicaoStatusResponse::de).toList());
    }
}
