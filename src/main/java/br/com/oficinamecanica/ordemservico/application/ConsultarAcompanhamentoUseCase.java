package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.AcompanhamentoNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.CodigoAcompanhamento;
import br.com.oficinamecanica.ordemservico.domain.DescricaoDoVeiculo;
import br.com.oficinamecanica.ordemservico.domain.ItemOrcamento;
import br.com.oficinamecanica.ordemservico.domain.ItemPeca;
import br.com.oficinamecanica.ordemservico.domain.ItemServico;
import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.Pecas;
import br.com.oficinamecanica.ordemservico.domain.Servicos;
import br.com.oficinamecanica.ordemservico.domain.Veiculos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConsultarAcompanhamentoUseCase {

    private final OrdemServicoRepository ordensServico;
    private final Veiculos veiculos;
    private final Servicos servicos;
    private final Pecas pecas;

    public ConsultarAcompanhamentoUseCase(OrdemServicoRepository ordensServico, Veiculos veiculos,
                                          Servicos servicos, Pecas pecas) {
        this.ordensServico = ordensServico;
        this.veiculos = veiculos;
        this.servicos = servicos;
        this.pecas = pecas;
    }

    @Transactional(readOnly = true)
    public Acompanhamento executar(String codigo) {
        OrdemServico ordemServico = ordemDoCodigo(ordensServico, codigo);
        Optional<Orcamento> versao = ordemServico.versaoMaisRecenteEnviada();
        return new Acompanhamento(
                ordemServico.status(),
                descricaoDoVeiculo(ordemServico.veiculoId()),
                versao,
                versao.map(orcamento -> itensDaVersao(ordemServico, orcamento.versao())).orElseGet(List::of),
                ordemServico.transicoes());
    }

    static OrdemServico ordemDoCodigo(OrdemServicoRepository ordensServico, String codigo) {
        return CodigoAcompanhamento.de(codigo)
                .flatMap(ordensServico::buscarPorCodigoAcompanhamento)
                .orElseThrow(AcompanhamentoNaoEncontradoException::new);
    }

    private DescricaoDoVeiculo descricaoDoVeiculo(UUID veiculoId) {
        return veiculos.descricaoDe(veiculoId).orElseThrow(AcompanhamentoNaoEncontradoException::new);
    }

    private List<ItemOrcamento> itensDaVersao(OrdemServico ordemServico, int versao) {
        List<ItemServico> itensServico = ordemServico.itensDeServicoDaVersao(versao);
        List<ItemPeca> itensPeca = ordemServico.itensDePecaDaVersao(versao);
        Map<UUID, String> nomesDeServicos = servicos.nomesDe(
                itensServico.stream().map(ItemServico::servicoId).toList());
        Map<UUID, String> nomesDePecas = pecas.nomesDe(
                itensPeca.stream().map(ItemPeca::pecaId).toList());

        List<ItemOrcamento> itens = new ArrayList<>();
        itensServico.forEach(item -> itens.add(ItemOrcamento.deServico(
                nomesDeServicos.get(item.servicoId()), item.valorMaoDeObraSnapshot())));
        itensPeca.forEach(item -> itens.add(ItemOrcamento.dePeca(
                nomesDePecas.get(item.pecaId()), item.quantidade(), item.precoSnapshot())));
        return List.copyOf(itens);
    }
}
