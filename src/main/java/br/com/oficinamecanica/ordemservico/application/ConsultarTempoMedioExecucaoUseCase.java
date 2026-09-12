package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConsultarTempoMedioExecucaoUseCase {

    private static final double SEGUNDOS_POR_HORA = 3600.0;

    private final OrdemServicoRepository ordensServico;

    public ConsultarTempoMedioExecucaoUseCase(OrdemServicoRepository ordensServico) {
        this.ordensServico = ordensServico;
    }

    public TempoMedioExecucao executar(Optional<UUID> servicoId) {
        List<Duration> tempos = ordensServico.listarComExecucaoConcluida(servicoId).stream()
                .map(OrdemServico::tempoEmExecucao)
                .flatMap(Optional::stream)
                .toList();
        return new TempoMedioExecucao(mediaEmHoras(tempos), tempos.size(), servicoId);
    }

    private Optional<Double> mediaEmHoras(List<Duration> tempos) {
        if (tempos.isEmpty()) {
            return Optional.empty();
        }
        double totalEmSegundos = tempos.stream().mapToLong(Duration::getSeconds).sum();
        return Optional.of(totalEmSegundos / tempos.size() / SEGUNDOS_POR_HORA);
    }
}
