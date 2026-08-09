package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class DetalharOrdemServicoUseCase {

    private final OrdemServicoRepository ordensServico;

    public DetalharOrdemServicoUseCase(OrdemServicoRepository ordensServico) {
        this.ordensServico = ordensServico;
    }

    @Transactional(readOnly = true)
    public OrdemServico executar(UUID id) {
        return ordensServico.buscarPorId(id).orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
    }
}
