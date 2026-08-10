package br.com.oficinamecanica.cadastro.infrastructure;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.Documento;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ClienteRepositoryAdapter implements ClienteRepository {

    private final ClienteJpaRepository repository;

    ClienteRepositoryAdapter(ClienteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        return repository.save(ClienteJpaEntity.de(cliente)).paraDominio();
    }

    @Override
    public Optional<Cliente> buscarAtivoPorId(UUID id) {
        return repository.findByIdAndAtivoTrue(id).map(ClienteJpaEntity::paraDominio);
    }

    @Override
    public Optional<Cliente> buscarAtivoParaInativacao(UUID id) {
        return repository.findComTravaByIdAndAtivoTrue(id).map(ClienteJpaEntity::paraDominio);
    }

    @Override
    public Optional<Cliente> buscarAtivoPorDocumento(Documento documento) {
        return repository.findByDocumentoAndAtivoTrue(documento.numero()).map(ClienteJpaEntity::paraDominio);
    }

    @Override
    public List<Cliente> listarAtivos() {
        return repository.findAllByAtivoTrueOrderByNomeAsc().stream().map(ClienteJpaEntity::paraDominio).toList();
    }

    @Override
    public boolean documentoJaCadastradoPorOutro(UUID id, Documento documento) {
        return repository.existsByDocumentoAndIdNot(documento.numero(), id);
    }
}
