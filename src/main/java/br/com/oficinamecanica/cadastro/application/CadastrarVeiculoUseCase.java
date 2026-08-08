package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.Placa;
import br.com.oficinamecanica.cadastro.domain.PlacaJaCadastradaException;
import br.com.oficinamecanica.cadastro.domain.Veiculo;
import br.com.oficinamecanica.cadastro.domain.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class CadastrarVeiculoUseCase {

    private final VeiculoRepository veiculos;
    private final ClienteRepository clientes;

    public CadastrarVeiculoUseCase(VeiculoRepository veiculos, ClienteRepository clientes) {
        this.veiculos = veiculos;
        this.clientes = clientes;
    }

    @Transactional
    public Veiculo executar(Placa placa, String marca, String modelo, int ano, UUID clienteId) {
        exigirClienteAtivo(clienteId);
        Veiculo veiculo = Veiculo.cadastrar(placa, marca, modelo, ano, clienteId);
        if (veiculos.placaJaCadastradaPorOutro(veiculo.id(), placa)) {
            throw new PlacaJaCadastradaException();
        }
        return veiculos.salvar(veiculo);
    }

    private void exigirClienteAtivo(UUID clienteId) {
        if (clientes.buscarAtivoPorId(clienteId).isEmpty()) {
            throw new ClienteNaoEncontradoException(clienteId);
        }
    }
}
