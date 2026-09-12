package br.com.oficinamecanica.ordemservico.domain;

import java.util.EnumSet;
import java.util.List;
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

    private static final Set<StatusOrdemServico> ENCERRADOS = EnumSet.of(FINALIZADA, ENTREGUE, CANCELADA);

    private static final List<StatusOrdemServico> PRIORIDADE_NA_FILA =
            List.of(EM_EXECUCAO, AGUARDANDO_APROVACAO, EM_DIAGNOSTICO, RECEBIDA);

    private static final Map<StatusOrdemServico, Set<StatusOrdemServico>> DESTINOS = Map.of(
            RECEBIDA, EnumSet.of(EM_DIAGNOSTICO),
            EM_DIAGNOSTICO, EnumSet.of(AGUARDANDO_APROVACAO),
            AGUARDANDO_APROVACAO, EnumSet.of(EM_EXECUCAO, CANCELADA),
            EM_EXECUCAO, EnumSet.of(AGUARDANDO_APROVACAO, FINALIZADA),
            FINALIZADA, EnumSet.of(ENTREGUE),
            ENTREGUE, EnumSet.noneOf(StatusOrdemServico.class),
            CANCELADA, EnumSet.noneOf(StatusOrdemServico.class));

    public static Set<StatusOrdemServico> emAndamento() {
        return EnumSet.of(RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, EM_EXECUCAO);
    }

    public static Set<StatusOrdemServico> encerrados() {
        return EnumSet.copyOf(ENCERRADOS);
    }

    public static Set<StatusOrdemServico> queAceitamItens() {
        return EnumSet.of(RECEBIDA, EM_DIAGNOSTICO);
    }

    public static Set<StatusOrdemServico> queAceitamDevolucaoDePecas() {
        return EnumSet.of(EM_EXECUCAO, FINALIZADA, ENTREGUE);
    }

    public int prioridadeNaFila() {
        int posicao = PRIORIDADE_NA_FILA.indexOf(this);
        if (posicao < 0) {
            return PRIORIDADE_NA_FILA.size();
        }
        return posicao;
    }

    public boolean aceitaTransicaoPara(StatusOrdemServico destino) {
        return DESTINOS.get(this).contains(destino);
    }
}
