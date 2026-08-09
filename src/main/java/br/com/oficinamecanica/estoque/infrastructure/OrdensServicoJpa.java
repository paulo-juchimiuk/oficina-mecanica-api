package br.com.oficinamecanica.estoque.infrastructure;

import br.com.oficinamecanica.estoque.domain.OrdensServico;
import br.com.oficinamecanica.estoque.domain.OrdensServicoEmAndamento;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component("ordensServicoDoEstoque")
class OrdensServicoJpa implements OrdensServico, OrdensServicoEmAndamento {

    private static final List<String> STATUS_EM_ANDAMENTO =
            List.of("RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "EM_EXECUCAO");
    private static final List<String> STATUS_QUE_ACEITAM_DEVOLUCAO =
            List.of("EM_EXECUCAO", "FINALIZADA", "ENTREGUE");
    private static final String EM_EXECUCAO = "EM_EXECUCAO";

    private static final String POR_PECA = """
            SELECT COUNT(*) FROM item_peca item
            JOIN ordem_servico ordem ON ordem.id = item.ordem_servico_id
            WHERE item.peca_id = :peca AND ordem.status IN (:status)
            """;

    private static final String POR_IDENTIDADE = "SELECT COUNT(*) FROM ordem_servico WHERE id = :ordem";

    private static final String POR_STATUS =
            "SELECT COUNT(*) FROM ordem_servico WHERE id = :ordem AND status IN (:status)";

    private final EntityManager entityManager;

    OrdensServicoJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existemParaPeca(UUID pecaId) {
        Number total = (Number) entityManager.createNativeQuery(POR_PECA)
                .setParameter("peca", pecaId)
                .setParameter("status", STATUS_EM_ANDAMENTO)
                .getSingleResult();
        return total.longValue() > 0;
    }

    @Override
    public boolean existe(UUID ordemServicoId) {
        Number total = (Number) entityManager.createNativeQuery(POR_IDENTIDADE)
                .setParameter("ordem", ordemServicoId)
                .getSingleResult();
        return total.longValue() > 0;
    }

    @Override
    public boolean estaEmExecucao(UUID ordemServicoId) {
        return temAlgumDosStatus(ordemServicoId, List.of(EM_EXECUCAO));
    }

    @Override
    public boolean aceitaDevolucaoDePecas(UUID ordemServicoId) {
        return temAlgumDosStatus(ordemServicoId, STATUS_QUE_ACEITAM_DEVOLUCAO);
    }

    private boolean temAlgumDosStatus(UUID ordemServicoId, List<String> status) {
        Number total = (Number) entityManager.createNativeQuery(POR_STATUS)
                .setParameter("ordem", ordemServicoId)
                .setParameter("status", status)
                .getSingleResult();
        return total.longValue() > 0;
    }
}
