package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.Placa;
import br.com.oficinamecanica.cadastro.domain.PlacaJaCadastradaException;
import br.com.oficinamecanica.cadastro.domain.Veiculo;
import br.com.oficinamecanica.cadastro.domain.VeiculoNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AlterarVeiculoUseCase {

    private final VeiculoRepository veiculos;
    private final ClienteRepository clientes;

    public AlterarVeiculoUseCase(VeiculoRepository veiculos, ClienteRepository clientes) {
        this.veiculos = veiculos;
        this.clientes = clientes;
    }

    @Transactional
    public Veiculo executar(UUID id, Placa placa, String marca, String modelo, int ano, UUID clienteId) {
        Veiculo veiculo = veiculos.buscarAtivoComTrava(id).orElseThrow(() -> new VeiculoNaoEncontradoException(id));
        if (clientes.buscarAtivoPorId(clienteId).isEmpty()) {
            throw new ClienteNaoEncontradoException(clienteId);
        }
        if (veiculos.placaJaCadastradaPorOutro(id, placa)) {
            throw new PlacaJaCadastradaException();
        }
        veiculo.alterar(placa, marca, modelo, ano, clienteId);
        return veiculos.salvar(veiculo);
    }
}
