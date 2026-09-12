package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import java.util.List;
import java.util.Optional;
import static java.util.Comparator.comparingInt;

public class ListarOrdensServicoUseCase {

    private final OrdemServicoRepository ordensServico;

    public ListarOrdensServicoUseCase(OrdemServicoRepository ordensServico) {
        this.ordensServico = ordensServico;
    }

    public List<OrdemServico> executar(Optional<StatusOrdemServico> status) {
        return status.map(ordensServico::listarComStatus)
                .orElseGet(this::filaDeAtendimento);
    }

    private List<OrdemServico> filaDeAtendimento() {
        return ordensServico.listarExceto(StatusOrdemServico.encerrados()).stream()
                .sorted(comparingInt((OrdemServico ordem) -> ordem.status().prioridadeNaFila())
                        .thenComparing(OrdemServico::criadaEm))
                .toList();
    }
}
