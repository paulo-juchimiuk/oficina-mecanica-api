package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import java.util.UUID;

public class DetalharOrdemServicoUseCase {

    private final OrdemServicoRepository ordensServico;

    public DetalharOrdemServicoUseCase(OrdemServicoRepository ordensServico) {
        this.ordensServico = ordensServico;
    }

    public OrdemServico executar(UUID id) {
        return ordensServico.buscarPorId(id).orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
    }
}
