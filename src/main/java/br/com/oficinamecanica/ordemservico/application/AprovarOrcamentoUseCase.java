package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprovarOrcamentoUseCase {

    private final OrdemServicoRepository ordensServico;
    private final ReservaDePecas reservaDePecas;
    private final ConsultarAcompanhamentoUseCase acompanhamento;

    public AprovarOrcamentoUseCase(OrdemServicoRepository ordensServico, ReservaDePecas reservaDePecas,
                                   ConsultarAcompanhamentoUseCase acompanhamento) {
        this.ordensServico = ordensServico;
        this.reservaDePecas = reservaDePecas;
        this.acompanhamento = acompanhamento;
    }

    @Transactional
    public Acompanhamento executar(String codigo) {
        OrdemServico ordemServico = ConsultarAcompanhamentoUseCase.ordemDoCodigo(ordensServico, codigo);
        int versaoAprovada = ordemServico.aprovarOrcamento().versao();
        ordensServico.salvar(ordemServico);
        reservaDePecas.reservar(ordemServico.id(), ordemServico.itensDePecaIntroduzidosPor(versaoAprovada));
        return acompanhamento.executar(codigo);
    }
}
