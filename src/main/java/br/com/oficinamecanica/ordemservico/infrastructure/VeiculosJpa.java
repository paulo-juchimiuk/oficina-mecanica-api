package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.DescricaoDoVeiculo;
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
    private static final String DESCRICAO =
            "SELECT marca, modelo, placa FROM veiculo WHERE id = :veiculo";

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

    @Override
    public Optional<DescricaoDoVeiculo> descricaoDe(UUID veiculoId) {
        List<?> encontrados = entityManager.createNativeQuery(DESCRICAO)
                .setParameter("veiculo", veiculoId)
                .getResultList();
        return encontrados.stream().findFirst().map(Object[].class::cast).map(VeiculosJpa::paraDescricao);
    }

    private static DescricaoDoVeiculo paraDescricao(Object[] linha) {
        return new DescricaoDoVeiculo((String) linha[0], (String) linha[1], (String) linha[2]);
    }
}
