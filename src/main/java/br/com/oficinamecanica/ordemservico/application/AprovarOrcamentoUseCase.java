package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;

public class AprovarOrcamentoUseCase {

    private final OrdemServicoRepository ordensServico;
    private final ReservaDePecas reservaDePecas;
    private final ConsultarAcompanhamentoUseCase acompanhamento;
    private final NotificadorDoCliente notificador;

    public AprovarOrcamentoUseCase(OrdemServicoRepository ordensServico, ReservaDePecas reservaDePecas,
                                   ConsultarAcompanhamentoUseCase acompanhamento, NotificadorDoCliente notificador) {
        this.ordensServico = ordensServico;
        this.reservaDePecas = reservaDePecas;
        this.acompanhamento = acompanhamento;
        this.notificador = notificador;
    }

    public Acompanhamento executar(String codigo) {
        OrdemServico ordemServico = ConsultarAcompanhamentoUseCase.ordemDoCodigoComTrava(ordensServico, codigo);
        int versaoAprovada = ordemServico.aprovarOrcamento().versao();
        OrdemServico gravada = ordensServico.salvar(ordemServico);
        notificador.notificarMudancaDeStatus(gravada);
        reservaDePecas.reservar(ordemServico.id(), ordemServico.itensDePecaIntroduzidosPor(versaoAprovada));
        return acompanhamento.executar(codigo);
    }
}
