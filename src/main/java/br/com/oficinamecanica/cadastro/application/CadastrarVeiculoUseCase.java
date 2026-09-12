package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.Placa;
import br.com.oficinamecanica.cadastro.domain.PlacaJaCadastradaException;
import br.com.oficinamecanica.cadastro.domain.Veiculo;
import br.com.oficinamecanica.cadastro.domain.VeiculoRepository;
import java.util.UUID;

public class CadastrarVeiculoUseCase {

    private final VeiculoRepository veiculos;
    private final ClienteRepository clientes;

    public CadastrarVeiculoUseCase(VeiculoRepository veiculos, ClienteRepository clientes) {
        this.veiculos = veiculos;
        this.clientes = clientes;
    }

    public Veiculo executar(Placa placa, String marca, String modelo, int ano, UUID clienteId) {
        exigirClienteAtivo(clienteId);
        Veiculo veiculo = Veiculo.cadastrar(placa, marca, modelo, ano, clienteId);
        if (veiculos.placaJaCadastradaPorOutro(veiculo.id(), placa)) {
            throw new PlacaJaCadastradaException();
        }
        return veiculos.salvar(veiculo);
    }

    private void exigirClienteAtivo(UUID clienteId) {
        if (clientes.buscarAtivoComTrava(clienteId).isEmpty()) {
            throw new ClienteNaoEncontradoException(clienteId);
        }
    }
}
