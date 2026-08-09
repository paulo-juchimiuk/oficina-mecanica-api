package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import java.util.UUID;

public class ReservaForaDaSituacaoEsperadaException extends ConflitoDeEstadoException {

    public ReservaForaDaSituacaoEsperadaException(UUID reservaId, SituacaoReserva atual, SituacaoReserva esperada) {
        super("RESERVA_FORA_DA_SITUACAO_ESPERADA",
                "Reserva de peca " + reservaId + " esta " + atual + " e a operacao exige " + esperada);
    }
}
