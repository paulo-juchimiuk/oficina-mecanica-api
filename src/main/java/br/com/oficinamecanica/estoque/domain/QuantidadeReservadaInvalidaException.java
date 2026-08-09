package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import java.util.UUID;

public class QuantidadeReservadaInvalidaException extends ConflitoDeEstadoException {

    public QuantidadeReservadaInvalidaException(UUID pecaId, String motivo) {
        super("QUANTIDADE_RESERVADA_INVALIDA",
                "Quantidade reservada da Peca " + pecaId + " " + motivo);
    }
}
