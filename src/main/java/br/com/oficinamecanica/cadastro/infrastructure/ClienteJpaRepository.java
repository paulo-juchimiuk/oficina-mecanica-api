package br.com.oficinamecanica.cadastro.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, UUID> {

    Optional<ClienteJpaEntity> findByIdAndAtivoTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ClienteJpaEntity> findComTravaByIdAndAtivoTrue(UUID id);

    Optional<ClienteJpaEntity> findByDocumentoAndAtivoTrue(String documento);

    List<ClienteJpaEntity> findAllByAtivoTrueOrderByNomeAsc();

    boolean existsByDocumentoAndIdNot(String documento, UUID id);
}
