package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class PecaComReservaAtivaException extends ConflitoDeEstadoException {

    public PecaComReservaAtivaException() {
        super("PECA_COM_RESERVA_ATIVA",
                "Peca tem Reserva de peca ATIVA e nao pode ser inativada");
    }
}
