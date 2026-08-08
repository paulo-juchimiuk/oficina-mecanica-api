package br.com.oficinamecanica.cadastro.infrastructure;

import br.com.oficinamecanica.cadastro.domain.Placa;
import br.com.oficinamecanica.cadastro.domain.Veiculo;
import br.com.oficinamecanica.cadastro.domain.VeiculoRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class VeiculoRepositoryAdapter implements VeiculoRepository {

    private final VeiculoJpaRepository repository;

    VeiculoRepositoryAdapter(VeiculoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Veiculo salvar(Veiculo veiculo) {
        return repository.save(VeiculoJpaEntity.de(veiculo)).paraDominio();
    }

    @Override
    public Optional<Veiculo> buscarAtivoPorId(UUID id) {
        return repository.findByIdAndAtivoTrue(id).map(VeiculoJpaEntity::paraDominio);
    }

    @Override
    public Optional<Veiculo> buscarAtivoPorPlaca(Placa placa) {
        return repository.findByPlacaAndAtivoTrue(placa.valor()).map(VeiculoJpaEntity::paraDominio);
    }

    @Override
    public List<Veiculo> listarAtivos() {
        return repository.findAllByAtivoTrueOrderByPlacaAsc().stream().map(VeiculoJpaEntity::paraDominio).toList();
    }

    @Override
    public boolean placaJaCadastradaPorOutro(UUID id, Placa placa) {
        return repository.existsByPlacaAndIdNot(placa.valor(), id);
    }
}
