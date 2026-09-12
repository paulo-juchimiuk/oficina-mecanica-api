package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.Placa;
import br.com.oficinamecanica.cadastro.domain.PlacaJaCadastradaException;
import br.com.oficinamecanica.cadastro.domain.Veiculo;
import br.com.oficinamecanica.cadastro.domain.VeiculoNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.VeiculoRepository;
import java.util.UUID;

public class AlterarVeiculoUseCase {

    private final VeiculoRepository veiculos;
    private final ClienteRepository clientes;

    public AlterarVeiculoUseCase(VeiculoRepository veiculos, ClienteRepository clientes) {
        this.veiculos = veiculos;
        this.clientes = clientes;
    }

    public Veiculo executar(UUID id, Placa placa, String marca, String modelo, int ano, UUID clienteId) {
        if (clientes.buscarAtivoComTrava(clienteId).isEmpty()) {
            throw new ClienteNaoEncontradoException(clienteId);
        }
        Veiculo veiculo = veiculos.buscarAtivoComTrava(id).orElseThrow(() -> new VeiculoNaoEncontradoException(id));
        if (veiculos.placaJaCadastradaPorOutro(id, placa)) {
            throw new PlacaJaCadastradaException();
        }
        veiculo.alterar(placa, marca, modelo, ano, clienteId);
        return veiculos.salvar(veiculo);
    }
}
