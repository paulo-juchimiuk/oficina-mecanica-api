package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.Dinheiro;
import br.com.oficinamecanica.ordemservico.domain.Servicos;
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
class ServicosJpa implements Servicos {

    private static final String VALOR_MAO_DE_OBRA =
            "SELECT valor_mao_de_obra, moeda FROM servico WHERE id = :servico AND ativo = TRUE";
    private static final String NOMES =
            "SELECT id, nome FROM servico WHERE id IN (:identificadores)";

    private final EntityManager entityManager;

    ServicosJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Dinheiro> valorMaoDeObraDe(UUID servicoId) {
        List<?> encontrados = entityManager.createNativeQuery(VALOR_MAO_DE_OBRA)
                .setParameter("servico", servicoId)
                .getResultList();
        return encontrados.stream().findFirst().map(Object[].class::cast).map(ServicosJpa::paraDinheiro);
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
