package br.com.oficinamecanica.cadastro.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface VeiculoJpaRepository extends JpaRepository<VeiculoJpaEntity, UUID> {

    Optional<VeiculoJpaEntity> findByIdAndAtivoTrue(UUID id);

    Optional<VeiculoJpaEntity> findByPlacaAndAtivoTrue(String placa);

    List<VeiculoJpaEntity> findAllByAtivoTrueOrderByPlacaAsc();

    boolean existsByPlacaAndIdNot(String placa, UUID id);
}
