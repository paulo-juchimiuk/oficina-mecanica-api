package br.com.oficinamecanica.cadastro.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeiculoRepository {

    Veiculo salvar(Veiculo veiculo);

    Optional<Veiculo> buscarAtivoPorId(UUID id);

    Optional<Veiculo> buscarAtivoComTrava(UUID id);

    Optional<Veiculo> buscarAtivoPorPlaca(Placa placa);

    List<Veiculo> listarAtivos();

    boolean placaJaCadastradaPorOutro(UUID id, Placa placa);
}
