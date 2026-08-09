package br.com.oficinamecanica.estoque.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

interface PendenciaPecaJpaRepository extends JpaRepository<PendenciaPecaJpaEntity, UUID> {

    List<PendenciaPecaJpaEntity> findAllByOrderByDetectadaEmAsc();

    List<PendenciaPecaJpaEntity> findAllByOrdemServicoIdOrderByDetectadaEmAsc(UUID ordemServicoId);
}
