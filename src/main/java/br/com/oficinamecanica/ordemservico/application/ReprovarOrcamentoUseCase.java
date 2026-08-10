package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReprovarOrcamentoUseCase {

    private final OrdemServicoRepository ordensServico;
    private final ConsultarAcompanhamentoUseCase acompanhamento;

    public ReprovarOrcamentoUseCase(OrdemServicoRepository ordensServico,
                                    ConsultarAcompanhamentoUseCase acompanhamento) {
        this.ordensServico = ordensServico;
        this.acompanhamento = acompanhamento;
    }

    @Transactional
    public Acompanhamento executar(String codigo) {
        OrdemServico ordemServico = ConsultarAcompanhamentoUseCase.ordemDoCodigoParaMovimentacao(ordensServico, codigo);
        ordemServico.reprovarOrcamento();
        ordensServico.salvar(ordemServico);
        return acompanhamento.executar(codigo);
    }
}
