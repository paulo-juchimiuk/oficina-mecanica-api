package br.com.oficinamecanica.cadastro.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, UUID> {

    Optional<ClienteJpaEntity> findByIdAndAtivoTrue(UUID id);

    Optional<ClienteJpaEntity> findByDocumentoAndAtivoTrue(String documento);

    List<ClienteJpaEntity> findAllByAtivoTrueOrderByNomeAsc();

    boolean existsByDocumentoAndIdNot(String documento, UUID id);
}
