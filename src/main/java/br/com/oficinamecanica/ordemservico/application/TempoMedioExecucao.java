package br.com.oficinamecanica.ordemservico.application;

import java.util.Optional;
import java.util.UUID;

public record TempoMedioExecucao(
        Optional<Double> tempoMedioHoras,
        int quantidadeOrdensConsideradas,
        Optional<UUID> servicoId) {
}
