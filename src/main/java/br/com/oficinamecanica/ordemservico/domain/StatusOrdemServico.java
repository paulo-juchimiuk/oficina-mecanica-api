package br.com.oficinamecanica.ordemservico.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum StatusOrdemServico {

    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA;

    private static final Map<StatusOrdemServico, Set<StatusOrdemServico>> DESTINOS = Map.of(
            RECEBIDA, EnumSet.of(EM_DIAGNOSTICO),
            EM_DIAGNOSTICO, EnumSet.of(AGUARDANDO_APROVACAO),
            AGUARDANDO_APROVACAO, EnumSet.of(EM_EXECUCAO, CANCELADA),
            EM_EXECUCAO, EnumSet.of(AGUARDANDO_APROVACAO, FINALIZADA),
            FINALIZADA, EnumSet.of(ENTREGUE),
            ENTREGUE, EnumSet.noneOf(StatusOrdemServico.class),
            CANCELADA, EnumSet.noneOf(StatusOrdemServico.class));

    public boolean aceitaTransicaoPara(StatusOrdemServico destino) {
        return DESTINOS.get(this).contains(destino);
    }

    public boolean encerrado() {
        return DESTINOS.get(this).isEmpty();
    }
}
