package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import java.util.List;
import java.util.UUID;

public class IncluirItensUseCase {

    private final OrdemServicoRepository ordensServico;
    private final PrecificadorDeItens precificador;

    public IncluirItensUseCase(OrdemServicoRepository ordensServico, PrecificadorDeItens precificador) {
        this.ordensServico = ordensServico;
        this.precificador = precificador;
    }

    public OrdemServico executar(UUID id, List<ItemDeServicoRequisitado> itensServico,
                                 List<ItemDePecaRequisitado> itensPeca) {
        OrdemServico ordemServico = ordensServico.buscarComTrava(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        ordemServico.incluirItens(
                precificador.precificarServicos(itensServico),
                precificador.precificarPecas(itensPeca));
        return ordensServico.salvar(ordemServico);
    }
}
