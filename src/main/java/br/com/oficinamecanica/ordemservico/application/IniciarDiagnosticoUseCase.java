package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class IniciarDiagnosticoUseCase {

    private final OrdemServicoRepository ordensServico;

    public IniciarDiagnosticoUseCase(OrdemServicoRepository ordensServico) {
        this.ordensServico = ordensServico;
    }

    @Transactional
    public OrdemServico executar(UUID id) {
        OrdemServico ordemServico = ordensServico.buscarPorId(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        ordemServico.iniciarDiagnostico();
        return ordensServico.salvar(ordemServico);
    }
}
