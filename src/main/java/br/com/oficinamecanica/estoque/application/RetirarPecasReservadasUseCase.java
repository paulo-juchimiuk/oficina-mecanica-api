package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.OrdemServicoForaDeExecucaoException;
import br.com.oficinamecanica.estoque.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.OrdensServico;
import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import br.com.oficinamecanica.estoque.domain.ReservaNaoEncontradaException;
import br.com.oficinamecanica.estoque.domain.ReservaPeca;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import static java.util.Comparator.comparing;

@Service
public class RetirarPecasReservadasUseCase {

    private final PecaRepository pecas;
    private final OrdensServico ordensServico;

    public RetirarPecasReservadasUseCase(PecaRepository pecas, OrdensServico ordensServico) {
        this.pecas = pecas;
        this.ordensServico = ordensServico;
    }

    @Transactional
    public List<ReservaPeca> executar(UUID ordemServicoId, List<UUID> reservaIds) {
        if (!ordensServico.existe(ordemServicoId)) {
            throw new OrdemServicoNaoEncontradaException(ordemServicoId);
        }
        if (!ordensServico.estaEmExecucao(ordemServicoId)) {
            throw new OrdemServicoForaDeExecucaoException(ordemServicoId);
        }
        return emOrdemDePeca(ordemServicoId, reservaIds).stream()
                .map(alvo -> retirar(ordemServicoId, alvo))
                .toList();
    }

    private List<ReservaPeca> emOrdemDePeca(UUID ordemServicoId, List<UUID> reservaIds) {
        return reservaIds.stream()
                .map(reservaId -> pecas.buscarReservaDaOrdemServico(ordemServicoId, reservaId)
                        .orElseThrow(() -> new ReservaNaoEncontradaException(reservaId, ordemServicoId)))
                .sorted(comparing(ReservaPeca::pecaId))
                .toList();
    }

    private ReservaPeca retirar(UUID ordemServicoId, ReservaPeca alvo) {
        Peca peca = pecas.buscarComTrava(alvo.pecaId())
                .orElseThrow(() -> new PecaNaoEncontradaException(alvo.pecaId()));
        ReservaPeca reserva = pecas.relerReservaDaOrdemServico(ordemServicoId, alvo.id())
                .orElseThrow(() -> new ReservaNaoEncontradaException(alvo.id(), ordemServicoId));
        peca.retirar(reserva);
        pecas.salvar(peca);
        return pecas.salvarReserva(reserva);
    }
}
