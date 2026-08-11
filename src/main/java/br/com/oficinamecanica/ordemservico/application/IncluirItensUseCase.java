package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.Dinheiro;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.PecaAIncluir;
import br.com.oficinamecanica.ordemservico.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.Pecas;
import br.com.oficinamecanica.ordemservico.domain.ServicoAIncluir;
import br.com.oficinamecanica.ordemservico.domain.ServicoNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Servicos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Service
public class IncluirItensUseCase {

    private final OrdemServicoRepository ordensServico;
    private final Servicos servicos;
    private final Pecas pecas;

    public IncluirItensUseCase(OrdemServicoRepository ordensServico, Servicos servicos, Pecas pecas) {
        this.ordensServico = ordensServico;
        this.servicos = servicos;
        this.pecas = pecas;
    }

    @Transactional
    public OrdemServico executar(UUID id, List<ItemDeServicoRequisitado> itensServico,
                                 List<ItemDePecaRequisitado> itensPeca) {
        OrdemServico ordemServico = ordensServico.buscarParaMovimentacao(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        ordemServico.incluirItens(resolverServicos(itensServico), resolverPecas(itensPeca));
        return ordensServico.salvar(ordemServico);
    }

    private List<ServicoAIncluir> resolverServicos(List<ItemDeServicoRequisitado> requisitados) {
        Map<UUID, Dinheiro> valores = valoresOrdenadosPorIdentificador(requisitados);
        return requisitados.stream()
                .map(item -> new ServicoAIncluir(item.servicoId(), valores.get(item.servicoId())))
                .toList();
    }

    private Map<UUID, Dinheiro> valoresOrdenadosPorIdentificador(List<ItemDeServicoRequisitado> requisitados) {
        return requisitados.stream()
                .map(ItemDeServicoRequisitado::servicoId)
                .distinct()
                .sorted()
                .collect(toMap(identity(), servicoId -> servicos.valorMaoDeObraDe(servicoId)
                        .orElseThrow(() -> new ServicoNaoEncontradoException(servicoId))));
    }

    private List<PecaAIncluir> resolverPecas(List<ItemDePecaRequisitado> requisitados) {
        Map<UUID, Dinheiro> precos = precosOrdenadosPorIdentificador(requisitados);
        return requisitados.stream()
                .map(item -> new PecaAIncluir(item.pecaId(), item.quantidade(), precos.get(item.pecaId())))
                .toList();
    }

    private Map<UUID, Dinheiro> precosOrdenadosPorIdentificador(List<ItemDePecaRequisitado> requisitados) {
        return requisitados.stream()
                .map(ItemDePecaRequisitado::pecaId)
                .distinct()
                .sorted()
                .collect(toMap(identity(), pecaId -> pecas.precoDe(pecaId)
                        .orElseThrow(() -> new PecaNaoEncontradaException(pecaId))));
    }
}
