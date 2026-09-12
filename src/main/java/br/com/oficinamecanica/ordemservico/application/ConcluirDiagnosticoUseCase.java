package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import java.util.UUID;

public class ConcluirDiagnosticoUseCase {

    private final OrdemServicoRepository ordensServico;
    private final NotificadorDoCliente notificador;

    public ConcluirDiagnosticoUseCase(OrdemServicoRepository ordensServico, NotificadorDoCliente notificador) {
        this.ordensServico = ordensServico;
        this.notificador = notificador;
    }

    public OrdemServico executar(UUID id) {
        OrdemServico ordemServico = ordensServico.buscarComTrava(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        Orcamento versaoEnviada = ordemServico.concluirDiagnostico();
        OrdemServico gravada = ordensServico.salvar(ordemServico);
        notificador.notificarOrcamento(gravada, versaoEnviada);
        return gravada;
    }
}
