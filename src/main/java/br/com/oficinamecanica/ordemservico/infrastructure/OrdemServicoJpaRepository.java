package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

interface OrdemServicoJpaRepository extends JpaRepository<OrdemServicoJpaEntity, UUID> {

    List<OrdemServicoJpaEntity> findAllByOrderByCriadaEmAsc();

    List<OrdemServicoJpaEntity> findAllByStatusOrderByCriadaEmAsc(StatusOrdemServico status);
}
