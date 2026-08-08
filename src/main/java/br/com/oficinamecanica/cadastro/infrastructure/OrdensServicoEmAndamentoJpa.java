package br.com.oficinamecanica.cadastro.infrastructure;

import br.com.oficinamecanica.cadastro.domain.OrdensServicoEmAndamento;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component
class OrdensServicoEmAndamentoJpa implements OrdensServicoEmAndamento {

    private static final List<String> STATUS_EM_ANDAMENTO =
            List.of("RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "EM_EXECUCAO");

    private static final String POR_CLIENTE =
            "SELECT COUNT(*) FROM ordem_servico WHERE cliente_id = :id AND status IN (:status)";
    private static final String POR_VEICULO =
            "SELECT COUNT(*) FROM ordem_servico WHERE veiculo_id = :id AND status IN (:status)";

    private final EntityManager entityManager;

    OrdensServicoEmAndamentoJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existemParaCliente(UUID clienteId) {
        return existeAlguma(POR_CLIENTE, clienteId);
    }

    @Override
    public boolean existemParaVeiculo(UUID veiculoId) {
        return existeAlguma(POR_VEICULO, veiculoId);
    }

    private boolean existeAlguma(String consulta, UUID id) {
        Number total = (Number) entityManager.createNativeQuery(consulta)
                .setParameter("id", id)
                .setParameter("status", STATUS_EM_ANDAMENTO)
                .getSingleResult();
        return total.longValue() > 0;
    }
}
