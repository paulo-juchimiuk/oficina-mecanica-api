package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.CodigoAcompanhamento;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class OrdemServicoRepositoryAdapter implements OrdemServicoRepository {

    private final OrdemServicoJpaRepository repository;

    OrdemServicoRepositoryAdapter(OrdemServicoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrdemServico salvar(OrdemServico ordemServico) {
        return repository.findById(ordemServico.id())
                .map(entidade -> atualizar(entidade, ordemServico))
                .orElseGet(() -> inserir(ordemServico))
                .paraDominio();
    }

    @Override
    public Optional<OrdemServico> buscarPorId(UUID id) {
        return repository.findById(id).map(OrdemServicoJpaEntity::paraDominio);
    }

    @Override
    public Optional<OrdemServico> buscarPorCodigoAcompanhamento(CodigoAcompanhamento codigoAcompanhamento) {
        return repository.findByCodigoAcompanhamento(codigoAcompanhamento.valor())
                .map(OrdemServicoJpaEntity::paraDominio);
    }

    @Override
    public List<OrdemServico> listar(Optional<StatusOrdemServico> status) {
        return status.map(repository::findAllByStatusOrderByCriadaEmAsc)
                .orElseGet(repository::findAllByOrderByCriadaEmAsc)
                .stream()
                .map(OrdemServicoJpaEntity::paraDominio)
                .toList();
    }

    private OrdemServicoJpaEntity atualizar(OrdemServicoJpaEntity entidade, OrdemServico ordemServico) {
        entidade.sincronizar(ordemServico);
        return entidade;
    }

    private OrdemServicoJpaEntity inserir(OrdemServico ordemServico) {
        return repository.save(OrdemServicoJpaEntity.de(ordemServico));
    }
}
