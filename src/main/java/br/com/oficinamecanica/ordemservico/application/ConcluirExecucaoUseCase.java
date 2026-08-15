package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ConcluirExecucaoUseCase {

    private final OrdemServicoRepository ordensServico;

    public ConcluirExecucaoUseCase(OrdemServicoRepository ordensServico) {
        this.ordensServico = ordensServico;
    }

    @Transactional
    public OrdemServico executar(UUID id) {
        OrdemServico ordemServico = ordensServico.buscarComTrava(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        ordemServico.concluirExecucao();
        return ordensServico.salvar(ordemServico);
    }
}
