package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.OrdensServico;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import br.com.oficinamecanica.estoque.domain.ReservaPeca;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class ConsultarPecasReservadasUseCase {

    private final PecaRepository pecas;
    private final OrdensServico ordensServico;

    public ConsultarPecasReservadasUseCase(PecaRepository pecas, OrdensServico ordensServico) {
        this.pecas = pecas;
        this.ordensServico = ordensServico;
    }

    @Transactional(readOnly = true)
    public List<ReservaPeca> executar(UUID ordemServicoId) {
        if (!ordensServico.existe(ordemServicoId)) {
            throw new OrdemServicoNaoEncontradaException(ordemServicoId);
        }
        return pecas.listarReservasDaOrdemServico(ordemServicoId);
    }
}
