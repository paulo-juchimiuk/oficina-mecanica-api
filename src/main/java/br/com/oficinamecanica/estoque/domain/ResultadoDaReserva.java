package br.com.oficinamecanica.estoque.domain;

import java.util.Optional;

public record ResultadoDaReserva(Optional<ReservaPeca> reserva, Optional<PendenciaPeca> pendencia) {
}
