package br.com.oficinamecanica.estoque.api;

import br.com.oficinamecanica.estoque.domain.PendenciaPeca;
import java.time.LocalDateTime;
import java.util.UUID;

public record PendenciaPecaResponse(
        UUID id,
        UUID ordemServicoId,
        UUID pecaId,
        String nomePeca,
        int quantidadeFaltante,
        LocalDateTime detectadaEm,
        LocalDateTime resolvidaEm) {

    static PendenciaPecaResponse de(PendenciaPeca pendencia) {
        return new PendenciaPecaResponse(
                pendencia.id(),
                pendencia.ordemServicoId(),
                pendencia.pecaId(),
                pendencia.nomePeca(),
                pendencia.quantidadeFaltante(),
                pendencia.detectadaEm(),
                pendencia.resolvidaEm());
    }
}
