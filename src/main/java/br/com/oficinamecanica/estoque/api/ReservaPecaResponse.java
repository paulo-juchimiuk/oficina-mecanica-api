package br.com.oficinamecanica.estoque.api;

import br.com.oficinamecanica.estoque.domain.ReservaPeca;
import br.com.oficinamecanica.estoque.domain.SituacaoReserva;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservaPecaResponse(
        UUID id,
        UUID pecaId,
        String nomePeca,
        int quantidade,
        SituacaoReserva situacao,
        LocalDateTime criadaEm) {

    static ReservaPecaResponse de(ReservaPeca reserva) {
        return new ReservaPecaResponse(
                reserva.id(),
                reserva.pecaId(),
                reserva.nomePeca(),
                reserva.quantidade(),
                reserva.situacao(),
                reserva.criadaEm());
    }
}
