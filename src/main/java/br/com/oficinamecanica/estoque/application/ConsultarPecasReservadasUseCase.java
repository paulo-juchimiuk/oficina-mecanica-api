package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.OrdensServico;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import br.com.oficinamecanica.estoque.domain.ReservaPeca;
import java.util.List;
import java.util.UUID;

public class ConsultarPecasReservadasUseCase {

    private final PecaRepository pecas;
    private final OrdensServico ordensServico;

    public ConsultarPecasReservadasUseCase(PecaRepository pecas, OrdensServico ordensServico) {
        this.pecas = pecas;
        this.ordensServico = ordensServico;
    }

    public List<ReservaPeca> executar(UUID ordemServicoId) {
        if (!ordensServico.existe(ordemServicoId)) {
            throw new OrdemServicoNaoEncontradaException(ordemServicoId);
        }
        return pecas.listarReservasDaOrdemServico(ordemServicoId);
    }
}
