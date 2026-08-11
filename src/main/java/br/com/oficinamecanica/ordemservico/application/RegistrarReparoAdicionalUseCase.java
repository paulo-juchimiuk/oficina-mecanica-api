package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Clientes;
import br.com.oficinamecanica.ordemservico.domain.Dinheiro;
import br.com.oficinamecanica.ordemservico.domain.Orcamento;
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
public class RegistrarReparoAdicionalUseCase {

    private final OrdemServicoRepository ordensServico;
    private final Servicos servicos;
    private final Pecas pecas;
    private final Clientes clientes;
    private final EnvioDeOrcamento envioDeOrcamento;

    public RegistrarReparoAdicionalUseCase(OrdemServicoRepository ordensServico, Servicos servicos, Pecas pecas,
                                           Clientes clientes, EnvioDeOrcamento envioDeOrcamento) {
        this.ordensServico = ordensServico;
        this.servicos = servicos;
        this.pecas = pecas;
        this.clientes = clientes;
        this.envioDeOrcamento = envioDeOrcamento;
    }

    @Transactional
    public OrdemServico executar(UUID id, String descricao, List<ItemDeServicoRequisitado> itensServico,
                                 List<ItemDePecaRequisitado> itensPeca) {
        OrdemServico ordemServico = ordensServico.buscarParaMovimentacao(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        Orcamento novaVersao = ordemServico.registrarReparoAdicional(
                descricao, resolverServicos(itensServico), resolverPecas(itensPeca));
        OrdemServico gravada = ordensServico.salvar(ordemServico);
        enviarAoCliente(gravada, novaVersao);
        return gravada;
    }

    private void enviarAoCliente(OrdemServico ordemServico, Orcamento versao) {
        String email = clientes.emailDe(ordemServico.clienteId())
                .orElseThrow(ClienteNaoEncontradoException::new);
        envioDeOrcamento.enviar(email, ordemServico.codigoAcompanhamento(), versao);
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
