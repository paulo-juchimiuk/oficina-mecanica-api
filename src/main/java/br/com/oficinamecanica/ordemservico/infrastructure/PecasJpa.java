package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.Dinheiro;
import br.com.oficinamecanica.ordemservico.domain.Pecas;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class PecasJpa implements Pecas {

    private static final String PRECO =
            "SELECT preco, moeda FROM peca WHERE id = :peca AND ativo = TRUE";

    private final EntityManager entityManager;

    PecasJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Dinheiro> precoDe(UUID pecaId) {
        List<?> encontrados = entityManager.createNativeQuery(PRECO)
                .setParameter("peca", pecaId)
                .getResultList();
        return encontrados.stream().findFirst().map(Object[].class::cast).map(PecasJpa::paraDinheiro);
    }

    private static Dinheiro paraDinheiro(Object[] linha) {
        return new Dinheiro((BigDecimal) linha[0], (String) linha[1]);
    }
}
