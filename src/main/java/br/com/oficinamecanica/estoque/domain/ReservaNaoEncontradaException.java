package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;
import java.util.UUID;

public class ReservaNaoEncontradaException extends RecursoNaoEncontradoException {

    public ReservaNaoEncontradaException(UUID reservaId, UUID ordemServicoId) {
        super("RESERVA_NAO_ENCONTRADA",
                "Reserva de peca " + reservaId + " nao encontrada na Ordem de Servico " + ordemServicoId);
    }
}
