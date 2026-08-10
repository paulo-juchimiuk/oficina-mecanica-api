package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.Dinheiro;
import br.com.oficinamecanica.ordemservico.domain.Pecas;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
class PecasJpa implements Pecas {

    private static final String PRECO =
            "SELECT preco, moeda FROM peca WHERE id = :peca AND ativo = TRUE FOR UPDATE";
    private static final String NOMES =
            "SELECT id, nome FROM peca WHERE id IN (:identificadores)";

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

    @Override
    public Map<UUID, String> nomesDe(Collection<UUID> identificadores) {
        if (identificadores.isEmpty()) {
            return Map.of();
        }
        List<?> encontrados = entityManager.createNativeQuery(NOMES)
                .setParameter("identificadores", identificadores)
                .getResultList();
        return encontrados.stream().map(Object[].class::cast)
                .collect(Collectors.toMap(linha -> (UUID) linha[0], linha -> (String) linha[1]));
    }

    private static Dinheiro paraDinheiro(Object[] linha) {
        return new Dinheiro((BigDecimal) linha[0], (String) linha[1]);
    }
}
