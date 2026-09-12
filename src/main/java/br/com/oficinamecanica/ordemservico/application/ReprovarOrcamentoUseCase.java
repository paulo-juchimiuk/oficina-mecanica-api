package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;

public class ReprovarOrcamentoUseCase {

    private final OrdemServicoRepository ordensServico;
    private final ConsultarAcompanhamentoUseCase acompanhamento;
    private final NotificadorDoCliente notificador;

    public ReprovarOrcamentoUseCase(OrdemServicoRepository ordensServico,
                                    ConsultarAcompanhamentoUseCase acompanhamento,
                                    NotificadorDoCliente notificador) {
        this.ordensServico = ordensServico;
        this.acompanhamento = acompanhamento;
        this.notificador = notificador;
    }

    public Acompanhamento executar(String codigo) {
        OrdemServico ordemServico = ConsultarAcompanhamentoUseCase.ordemDoCodigoComTrava(ordensServico, codigo);
        ordemServico.reprovarOrcamento();
        notificador.notificarMudancaDeStatus(ordensServico.salvar(ordemServico));
        return acompanhamento.executar(codigo);
    }
}
