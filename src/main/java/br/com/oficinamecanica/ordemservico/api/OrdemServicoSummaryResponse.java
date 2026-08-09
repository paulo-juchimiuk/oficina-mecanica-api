package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrdemServicoSummaryResponse(
        UUID id,
        StatusOrdemServico status,
        UUID clienteId,
        UUID veiculoId,
        LocalDateTime criadaEm) {

    static OrdemServicoSummaryResponse de(OrdemServico ordemServico) {
        return new OrdemServicoSummaryResponse(
                ordemServico.id(),
                ordemServico.status(),
                ordemServico.clienteId(),
                ordemServico.veiculoId(),
                ordemServico.criadaEm());
    }
}
