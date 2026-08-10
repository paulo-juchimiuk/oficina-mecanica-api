package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrdemServicoJpaRepository extends JpaRepository<OrdemServicoJpaEntity, UUID> {

    Optional<OrdemServicoJpaEntity> findByCodigoAcompanhamento(String codigoAcompanhamento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ordem FROM OrdemServicoJpaEntity ordem WHERE ordem.id = :id")
    Optional<OrdemServicoJpaEntity> findComTravaById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ordem FROM OrdemServicoJpaEntity ordem WHERE ordem.codigoAcompanhamento = :codigo")
    Optional<OrdemServicoJpaEntity> findComTravaByCodigoAcompanhamento(@Param("codigo") String codigo);

    boolean existsByClienteIdAndStatusIn(UUID clienteId, Collection<StatusOrdemServico> status);

    boolean existsByVeiculoIdAndStatusIn(UUID veiculoId, Collection<StatusOrdemServico> status);

    boolean existsByIdAndStatusIn(UUID id, Collection<StatusOrdemServico> status);

    @Query("""
            SELECT COUNT(item) > 0 FROM OrdemServicoJpaEntity ordem
            JOIN ordem.itensServico item
            WHERE item.servicoId = :servico AND ordem.status IN :status
            """)
    boolean existeComServico(@Param("servico") UUID servicoId,
                             @Param("status") Collection<StatusOrdemServico> status);

    @Query("""
            SELECT COUNT(item) > 0 FROM OrdemServicoJpaEntity ordem
            JOIN ordem.itensPeca item
            WHERE item.pecaId = :peca AND ordem.status IN :status
            """)
    boolean existeComPeca(@Param("peca") UUID pecaId,
                          @Param("status") Collection<StatusOrdemServico> status);

    List<OrdemServicoJpaEntity> findAllByOrderByCriadaEmAsc();

    List<OrdemServicoJpaEntity> findAllByStatusOrderByCriadaEmAsc(StatusOrdemServico status);

    @Query(value = """
            SELECT ordem.* FROM ordem_servico ordem
            WHERE EXISTS (SELECT 1 FROM transicao_status transicao
                          WHERE transicao.ordem_servico_id = ordem.id
                          AND transicao.para_status = 'FINALIZADA')
            AND (CAST(:servico AS UUID) IS NULL OR EXISTS (
                          SELECT 1 FROM item_servico item
                          WHERE item.ordem_servico_id = ordem.id
                          AND item.servico_id = CAST(:servico AS UUID)))
            ORDER BY ordem.criada_em
            """, nativeQuery = true)
    List<OrdemServicoJpaEntity> listarComExecucaoConcluida(@Param("servico") String servico);
}
