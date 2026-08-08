package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Veiculo;
import br.com.oficinamecanica.cadastro.domain.VeiculoNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class DetalharVeiculoUseCase {

    private final VeiculoRepository veiculos;

    public DetalharVeiculoUseCase(VeiculoRepository veiculos) {
        this.veiculos = veiculos;
    }

    @Transactional(readOnly = true)
    public Veiculo executar(UUID id) {
        return veiculos.buscarAtivoPorId(id).orElseThrow(() -> new VeiculoNaoEncontradoException(id));
    }
}
