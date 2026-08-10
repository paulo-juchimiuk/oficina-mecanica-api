package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.estoque.domain.OrdensServico;
import br.com.oficinamecanica.estoque.domain.OrdensServicoEmAndamento;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.UUID;

@Component
class OrdensServicoDoEstoque implements OrdensServico, OrdensServicoEmAndamento {

    private final OrdemServicoJpaRepository repository;

    OrdensServicoDoEstoque(OrdemServicoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existemParaPeca(UUID pecaId) {
        return repository.existeComPeca(pecaId, StatusOrdemServico.emAndamento());
    }

    @Override
    public boolean existe(UUID ordemServicoId) {
        return repository.existsById(ordemServicoId);
    }

    @Override
    public boolean estaEmExecucao(UUID ordemServicoId) {
        return temStatusSobTrava(ordemServicoId, Set.of(StatusOrdemServico.EM_EXECUCAO));
    }

    @Override
    public boolean aceitaDevolucaoDePecas(UUID ordemServicoId) {
        return temStatusSobTrava(ordemServicoId, StatusOrdemServico.queAceitamDevolucaoDePecas());
    }

    private boolean temStatusSobTrava(UUID ordemServicoId, Set<StatusOrdemServico> aceitos) {
        return repository.findComTravaById(ordemServicoId)
                .map(OrdemServicoJpaEntity::status)
                .filter(aceitos::contains)
                .isPresent();
    }
}
