package br.com.oficinamecanica.estoque.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PecaJpaRepository extends JpaRepository<PecaJpaEntity, UUID> {

    Optional<PecaJpaEntity> findByIdAndAtivoTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PecaJpaEntity> findComTravaByIdAndAtivoTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PecaJpaEntity> findComTravaById(UUID id);

    List<PecaJpaEntity> findAllByAtivoTrueOrderByNomeAsc();

    @Query("SELECT peca FROM PecaJpaEntity peca "
            + "WHERE peca.ativo = TRUE AND peca.saldoEmEstoque < peca.estoqueMinimo ORDER BY peca.nome ASC")
    List<PecaJpaEntity> listarAtivasAbaixoDoEstoqueMinimo();
}
