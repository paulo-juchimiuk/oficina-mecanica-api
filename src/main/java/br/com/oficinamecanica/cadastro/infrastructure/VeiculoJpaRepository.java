package br.com.oficinamecanica.cadastro.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface VeiculoJpaRepository extends JpaRepository<VeiculoJpaEntity, UUID> {

    Optional<VeiculoJpaEntity> findByIdAndAtivoTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VeiculoJpaEntity> findComTravaByIdAndAtivoTrue(UUID id);

    Optional<VeiculoJpaEntity> findByPlacaAndAtivoTrue(String placa);

    List<VeiculoJpaEntity> findAllByAtivoTrueOrderByPlacaAsc();

    boolean existsByPlacaAndIdNot(String placa, UUID id);
}
