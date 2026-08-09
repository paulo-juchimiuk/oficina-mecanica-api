package br.com.oficinamecanica.estoque.infrastructure;

import br.com.oficinamecanica.estoque.domain.SituacaoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ReservaPecaJpaRepository extends JpaRepository<ReservaPecaJpaEntity, UUID> {

    Optional<ReservaPecaJpaEntity> findByIdAndOrdemServicoId(UUID id, UUID ordemServicoId);

    List<ReservaPecaJpaEntity> findAllByOrdemServicoIdOrderByCriadaEmAsc(UUID ordemServicoId);

    boolean existsByPecaIdAndSituacao(UUID pecaId, SituacaoReserva situacao);
}
