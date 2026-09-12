package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import java.util.Set;
import static java.util.stream.Collectors.joining;

public class EstadoExigidoException extends ConflitoDeEstadoException {

    public EstadoExigidoException(StatusOrdemServico atual, StatusOrdemServico exigido) {
        super("TRANSICAO_INVALIDA", mensagem(atual, exigido.name()));
    }

    public EstadoExigidoException(StatusOrdemServico atual, Set<StatusOrdemServico> exigidos) {
        super("TRANSICAO_INVALIDA", mensagem(atual, exigidos.stream()
                .map(StatusOrdemServico::name)
                .collect(joining(" ou "))));
    }

    private static String mensagem(StatusOrdemServico atual, String exigidos) {
        return "A Ordem de Servico esta em " + atual + " e esta operacao exige que ela esteja em " + exigidos;
    }
}
