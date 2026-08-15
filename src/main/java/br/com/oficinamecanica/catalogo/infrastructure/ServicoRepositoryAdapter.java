package br.com.oficinamecanica.catalogo.infrastructure;

import br.com.oficinamecanica.catalogo.domain.Servico;
import br.com.oficinamecanica.catalogo.domain.ServicoRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ServicoRepositoryAdapter implements ServicoRepository {

    private final ServicoJpaRepository repository;

    ServicoRepositoryAdapter(ServicoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Servico salvar(Servico servico) {
        return repository.save(ServicoJpaEntity.de(servico)).paraDominio();
    }

    @Override
    public Optional<Servico> buscarAtivoPorId(UUID id) {
        return repository.findByIdAndAtivoTrue(id).map(ServicoJpaEntity::paraDominio);
    }

    @Override
    public Optional<Servico> buscarAtivoComTrava(UUID id) {
        return repository.findComTravaByIdAndAtivoTrue(id).map(ServicoJpaEntity::paraDominio);
    }

    @Override
    public List<Servico> listarAtivos() {
        return repository.findAllByAtivoTrueOrderByNomeAsc().stream().map(ServicoJpaEntity::paraDominio).toList();
    }
}
