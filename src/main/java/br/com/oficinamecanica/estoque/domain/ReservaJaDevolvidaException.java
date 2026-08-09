package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import java.util.UUID;

public class ReservaJaDevolvidaException extends ConflitoDeEstadoException {

    public ReservaJaDevolvidaException(UUID reservaId) {
        super("RESERVA_JA_DEVOLVIDA",
                "Reserva de peca " + reservaId + " ja foi devolvida");
    }
}
