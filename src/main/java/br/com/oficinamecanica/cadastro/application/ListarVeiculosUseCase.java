package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Placa;
import br.com.oficinamecanica.cadastro.domain.Veiculo;
import br.com.oficinamecanica.cadastro.domain.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class ListarVeiculosUseCase {

    private final VeiculoRepository veiculos;

    public ListarVeiculosUseCase(VeiculoRepository veiculos) {
        this.veiculos = veiculos;
    }

    @Transactional(readOnly = true)
    public List<Veiculo> executar(Optional<Placa> placa) {
        if (placa.isEmpty()) {
            return veiculos.listarAtivos();
        }
        return veiculos.buscarAtivoPorPlaca(placa.get()).map(List::of).orElseGet(List::of);
    }
}
