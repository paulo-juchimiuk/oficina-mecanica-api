package br.com.oficinamecanica.estoque.infrastructure;

import br.com.oficinamecanica.estoque.domain.Peca;
import br.com.oficinamecanica.estoque.domain.PecaRepository;
import br.com.oficinamecanica.estoque.domain.PendenciaPeca;
import br.com.oficinamecanica.estoque.domain.ReservaPeca;
import br.com.oficinamecanica.estoque.domain.SituacaoReserva;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class PecaRepositoryAdapter implements PecaRepository {

    private final PecaJpaRepository pecas;
    private final ReservaPecaJpaRepository reservas;
    private final PendenciaPecaJpaRepository pendencias;
    private final EntityManager entityManager;

    PecaRepositoryAdapter(PecaJpaRepository pecas, ReservaPecaJpaRepository reservas,
                          PendenciaPecaJpaRepository pendencias, EntityManager entityManager) {
        this.pecas = pecas;
        this.reservas = reservas;
        this.pendencias = pendencias;
        this.entityManager = entityManager;
    }

    @Override
    public Peca salvar(Peca peca) {
        return pecas.save(PecaJpaEntity.de(peca)).paraDominio();
    }

    @Override
    public Optional<Peca> buscarAtivaPorId(UUID id) {
        return pecas.findByIdAndAtivoTrue(id).map(PecaJpaEntity::paraDominio);
    }

    @Override
    public Optional<Peca> buscarAtivaComTrava(UUID id) {
        return pecas.findComTravaByIdAndAtivoTrue(id).map(this::relerSobTrava).map(PecaJpaEntity::paraDominio);
    }

    @Override
    public Optional<Peca> buscarComTrava(UUID id) {
        return pecas.findComTravaById(id).map(this::relerSobTrava).map(PecaJpaEntity::paraDominio);
    }

    @Override
    public List<Peca> listarAtivas() {
        return pecas.findAllByAtivoTrueOrderByNomeAsc().stream().map(PecaJpaEntity::paraDominio).toList();
    }

    @Override
    public List<Peca> listarAtivasAbaixoDoEstoqueMinimo() {
        return pecas.listarAtivasAbaixoDoEstoqueMinimo().stream().map(PecaJpaEntity::paraDominio).toList();
    }

    @Override
    public boolean temReservaAtiva(UUID pecaId) {
        return reservas.existsByPecaIdAndSituacao(pecaId, SituacaoReserva.ATIVA);
    }

    @Override
    public ReservaPeca salvarReserva(ReservaPeca reserva) {
        Optional<ReservaPecaJpaEntity> existente = reservas.findById(reserva.id());
        if (existente.isPresent()) {
            existente.get().mover(reserva.situacao());
            return reserva;
        }
        reservas.save(ReservaPecaJpaEntity.de(reserva, referenciaDaPeca(reserva.pecaId())));
        return reserva;
    }

    @Override
    public Optional<ReservaPeca> buscarReservaDaOrdemServico(UUID ordemServicoId, UUID reservaId) {
        return reservas.findByIdAndOrdemServicoId(reservaId, ordemServicoId)
                .map(ReservaPecaJpaEntity::paraDominio);
    }

    @Override
    public Optional<ReservaPeca> buscarReservaComTrava(UUID ordemServicoId, UUID reservaId) {
        return reservas.findByIdAndOrdemServicoId(reservaId, ordemServicoId)
                .map(this::relerDoBanco)
                .map(ReservaPecaJpaEntity::paraDominio);
    }

    @Override
    public List<ReservaPeca> listarReservasDaOrdemServico(UUID ordemServicoId) {
        return reservas.findAllByOrdemServicoIdOrderByCriadaEmAsc(ordemServicoId).stream()
                .map(ReservaPecaJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public PendenciaPeca salvarPendencia(PendenciaPeca pendencia) {
        pendencias.save(PendenciaPecaJpaEntity.de(pendencia, referenciaDaPeca(pendencia.pecaId())));
        return pendencia;
    }

    @Override
    public List<PendenciaPeca> listarPendencias() {
        return pendencias.findAllByOrderByDetectadaEmAsc().stream()
                .map(PendenciaPecaJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public List<PendenciaPeca> listarPendenciasDaOrdemServico(UUID ordemServicoId) {
        return pendencias.findAllByOrdemServicoIdOrderByDetectadaEmAsc(ordemServicoId).stream()
                .map(PendenciaPecaJpaEntity::paraDominio)
                .toList();
    }

    private PecaJpaEntity referenciaDaPeca(UUID pecaId) {
        return entityManager.find(PecaJpaEntity.class, pecaId);
    }

    private PecaJpaEntity relerSobTrava(PecaJpaEntity entidade) {
        entityManager.refresh(entidade, LockModeType.PESSIMISTIC_WRITE);
        return entidade;
    }

    private ReservaPecaJpaEntity relerDoBanco(ReservaPecaJpaEntity entidade) {
        entityManager.refresh(entidade);
        return entidade;
    }
}
