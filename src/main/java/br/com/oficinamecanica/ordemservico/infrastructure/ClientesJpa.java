package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.Clientes;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ClientesJpa implements Clientes {

    private static final String POR_DOCUMENTO =
            "SELECT id FROM cliente WHERE documento = :documento AND ativo = TRUE";

    private final EntityManager entityManager;

    ClientesJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<UUID> identidadePorDocumento(String documento) {
        List<?> encontrados = entityManager.createNativeQuery(POR_DOCUMENTO)
                .setParameter("documento", documento)
                .getResultList();
        return encontrados.stream().findFirst().map(UUID.class::cast);
    }
}
