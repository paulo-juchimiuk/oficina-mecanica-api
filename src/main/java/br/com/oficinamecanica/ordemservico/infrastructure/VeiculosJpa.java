package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.Veiculos;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class VeiculosJpa implements Veiculos {

    private static final String PROPRIETARIO =
            "SELECT cliente_id FROM veiculo WHERE id = :veiculo AND ativo = TRUE";

    private final EntityManager entityManager;

    VeiculosJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<UUID> proprietarioDe(UUID veiculoId) {
        List<?> encontrados = entityManager.createNativeQuery(PROPRIETARIO)
                .setParameter("veiculo", veiculoId)
                .getResultList();
        return encontrados.stream().findFirst().map(UUID.class::cast);
    }
}
