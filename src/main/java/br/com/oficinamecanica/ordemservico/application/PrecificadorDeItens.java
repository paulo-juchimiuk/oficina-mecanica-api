package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.Dinheiro;
import br.com.oficinamecanica.ordemservico.domain.PecaAIncluir;
import br.com.oficinamecanica.ordemservico.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.Pecas;
import br.com.oficinamecanica.ordemservico.domain.ServicoAIncluir;
import br.com.oficinamecanica.ordemservico.domain.ServicoNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Servicos;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class PrecificadorDeItens {

    private final Servicos servicos;
    private final Pecas pecas;

    public PrecificadorDeItens(Servicos servicos, Pecas pecas) {
        this.servicos = servicos;
        this.pecas = pecas;
    }

    public List<ServicoAIncluir> precificarServicos(List<ItemDeServicoRequisitado> requisitados) {
        Map<UUID, Dinheiro> valores = valoresOrdenadosPorIdentificador(requisitados);
        return requisitados.stream()
                .map(item -> new ServicoAIncluir(item.servicoId(), valores.get(item.servicoId())))
                .toList();
    }

    public List<PecaAIncluir> precificarPecas(List<ItemDePecaRequisitado> requisitados) {
        Map<UUID, Dinheiro> precos = precosOrdenadosPorIdentificador(requisitados);
        return requisitados.stream()
                .map(item -> new PecaAIncluir(item.pecaId(), item.quantidade(), precos.get(item.pecaId())))
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

    private Map<UUID, Dinheiro> precosOrdenadosPorIdentificador(List<ItemDePecaRequisitado> requisitados) {
        return requisitados.stream()
                .map(ItemDePecaRequisitado::pecaId)
                .distinct()
                .sorted()
                .collect(toMap(identity(), pecaId -> pecas.precoDe(pecaId)
                        .orElseThrow(() -> new PecaNaoEncontradaException(pecaId))));
    }
}
