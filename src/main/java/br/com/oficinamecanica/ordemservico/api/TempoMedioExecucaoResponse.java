package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.ordemservico.application.TempoMedioExecucao;
import java.util.UUID;

public record TempoMedioExecucaoResponse(
        Double tempoMedioHoras,
        int quantidadeOSsConsideradas,
        String recorte,
        UUID servicoId) {

    private static final String RECORTE = "EM_EXECUCAO_A_FINALIZADA";

    static TempoMedioExecucaoResponse de(TempoMedioExecucao metrica) {
        return new TempoMedioExecucaoResponse(
                metrica.tempoMedioHoras().orElse(null),
                metrica.quantidadeOrdensConsideradas(),
                RECORTE,
                metrica.servicoId().orElse(null));
    }
}
