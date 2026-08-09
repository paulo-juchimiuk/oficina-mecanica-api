package br.com.oficinamecanica.catalogo.infrastructure;

import br.com.oficinamecanica.catalogo.domain.OrdensServicoEmAndamento;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component("ordensServicoEmAndamentoDoCatalogo")
class OrdensServicoEmAndamentoJpa implements OrdensServicoEmAndamento {

    private static final List<String> STATUS_EM_ANDAMENTO =
            List.of("RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "EM_EXECUCAO");

    private static final String POR_SERVICO = """
            SELECT COUNT(*) FROM item_servico item
            JOIN ordem_servico ordem ON ordem.id = item.ordem_servico_id
            WHERE item.servico_id = :id AND ordem.status IN (:status)
            """;

    private final EntityManager entityManager;

    OrdensServicoEmAndamentoJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existemParaServico(UUID servicoId) {
        Number total = (Number) entityManager.createNativeQuery(POR_SERVICO)
                .setParameter("id", servicoId)
                .setParameter("status", STATUS_EM_ANDAMENTO)
                .getSingleResult();
        return total.longValue() > 0;
    }
}
