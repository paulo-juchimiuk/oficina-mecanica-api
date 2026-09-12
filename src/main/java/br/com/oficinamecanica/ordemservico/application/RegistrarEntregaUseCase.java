package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import java.util.UUID;

public class RegistrarEntregaUseCase {

    private final OrdemServicoRepository ordensServico;
    private final NotificadorDoCliente notificador;

    public RegistrarEntregaUseCase(OrdemServicoRepository ordensServico, NotificadorDoCliente notificador) {
        this.ordensServico = ordensServico;
        this.notificador = notificador;
    }

    public OrdemServico executar(UUID id) {
        OrdemServico ordemServico = ordensServico.buscarComTrava(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        ordemServico.registrarEntrega();
        OrdemServico gravada = ordensServico.salvar(ordemServico);
        notificador.notificarMudancaDeStatus(gravada);
        return gravada;
    }
}
