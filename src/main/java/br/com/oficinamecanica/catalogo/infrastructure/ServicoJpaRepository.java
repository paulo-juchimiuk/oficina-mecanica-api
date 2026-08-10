package br.com.oficinamecanica.catalogo.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ServicoJpaRepository extends JpaRepository<ServicoJpaEntity, UUID> {

    Optional<ServicoJpaEntity> findByIdAndAtivoTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ServicoJpaEntity> findComTravaByIdAndAtivoTrue(UUID id);

    List<ServicoJpaEntity> findAllByAtivoTrueOrderByNomeAsc();
}
