package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import java.util.List;
import java.util.UUID;

public class RegistrarReparoAdicionalUseCase {

    private final OrdemServicoRepository ordensServico;
    private final PrecificadorDeItens precificador;
    private final NotificadorDoCliente notificador;

    public RegistrarReparoAdicionalUseCase(OrdemServicoRepository ordensServico, PrecificadorDeItens precificador,
                                           NotificadorDoCliente notificador) {
        this.ordensServico = ordensServico;
        this.precificador = precificador;
        this.notificador = notificador;
    }

    public OrdemServico executar(UUID id, String descricao, List<ItemDeServicoRequisitado> itensServico,
                                 List<ItemDePecaRequisitado> itensPeca) {
        OrdemServico ordemServico = ordensServico.buscarComTrava(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        Orcamento novaVersao = ordemServico.registrarReparoAdicional(
                descricao,
                precificador.precificarServicos(itensServico),
                precificador.precificarPecas(itensPeca));
        OrdemServico gravada = ordensServico.salvar(ordemServico);
        notificador.notificarOrcamento(gravada, novaVersao);
        return gravada;
    }
}
