package br.com.oficinamecanica.estoque.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PecaRepository {

    Peca salvar(Peca peca);

    Optional<Peca> buscarAtivaPorId(UUID id);

    Optional<Peca> buscarAtivaComTrava(UUID id);

    Optional<Peca> buscarComTrava(UUID id);

    List<Peca> listarAtivas();

    List<Peca> listarAtivasAbaixoDoEstoqueMinimo();

    boolean temReservaAtiva(UUID pecaId);

    ReservaPeca salvarReserva(ReservaPeca reserva);

    Optional<ReservaPeca> buscarReservaDaOrdemServico(UUID ordemServicoId, UUID reservaId);

    Optional<ReservaPeca> relerReservaDaOrdemServico(UUID ordemServicoId, UUID reservaId);

    List<ReservaPeca> listarReservasDaOrdemServico(UUID ordemServicoId);

    PendenciaPeca salvarPendencia(PendenciaPeca pendencia);

    List<PendenciaPeca> listarPendencias();

    List<PendenciaPeca> listarPendenciasDaOrdemServico(UUID ordemServicoId);
}
