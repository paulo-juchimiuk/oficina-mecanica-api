package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.CodigoAcompanhamento;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class OrdemServicoRepositoryAdapter implements OrdemServicoRepository {

    private final OrdemServicoJpaRepository repository;
    private final EntityManager entityManager;

    OrdemServicoRepositoryAdapter(OrdemServicoJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
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
    public Optional<OrdemServico> buscarComTrava(UUID id) {
        return repository.findComTravaById(id).map(this::relerSobTrava).map(OrdemServicoJpaEntity::paraDominio);
    }

    @Override
    public Optional<OrdemServico> buscarComTravaPorCodigo(CodigoAcompanhamento codigoAcompanhamento) {
        return repository.findComTravaByCodigoAcompanhamento(codigoAcompanhamento.valor())
                .map(this::relerSobTrava)
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

    @Override
    public List<OrdemServico> listarComExecucaoConcluida(Optional<UUID> servicoId) {
        return repository.listarComExecucaoConcluida(servicoId.map(UUID::toString).orElse(null)).stream()
                .map(OrdemServicoJpaEntity::paraDominio)
                .toList();
    }

    private OrdemServicoJpaEntity relerSobTrava(OrdemServicoJpaEntity entidade) {
        entityManager.refresh(entidade, LockModeType.PESSIMISTIC_WRITE);
        return entidade;
    }

    private OrdemServicoJpaEntity atualizar(OrdemServicoJpaEntity entidade, OrdemServico ordemServico) {
        entidade.sincronizar(ordemServico);
        return entidade;
    }

    private OrdemServicoJpaEntity inserir(OrdemServico ordemServico) {
        return repository.save(OrdemServicoJpaEntity.de(ordemServico));
    }
}
