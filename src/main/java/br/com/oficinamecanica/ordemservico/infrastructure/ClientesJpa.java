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
            "SELECT id FROM cliente WHERE documento = :documento AND ativo = TRUE FOR UPDATE";
    private static final String EMAIL_POR_IDENTIDADE =
            "SELECT email FROM cliente WHERE id = :cliente";

    private final EntityManager entityManager;

    ClientesJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<UUID> identidadePorDocumento(String documento) {
        return primeiro(POR_DOCUMENTO, "documento", documento).map(UUID.class::cast);
    }

    @Override
    public Optional<String> emailDe(UUID clienteId) {
        return primeiro(EMAIL_POR_IDENTIDADE, "cliente", clienteId).map(String.class::cast);
    }

    private Optional<?> primeiro(String consulta, String parametro, Object valor) {
        List<?> encontrados = entityManager.createNativeQuery(consulta)
                .setParameter(parametro, valor)
                .getResultList();
        return encontrados.stream().findFirst();
    }
}
