package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class ListarOrdensServicoUseCase {

    private final OrdemServicoRepository ordensServico;

    public ListarOrdensServicoUseCase(OrdemServicoRepository ordensServico) {
        this.ordensServico = ordensServico;
    }

    @Transactional(readOnly = true)
    public List<OrdemServico> executar(Optional<StatusOrdemServico> status) {
        return ordensServico.listar(status);
    }
}
