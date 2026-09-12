package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import br.com.oficinamecanica.cadastro.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import java.util.UUID;

public class DetalharClienteUseCase {

    private final ClienteRepository clientes;

    public DetalharClienteUseCase(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    public Cliente executar(UUID id) {
        return clientes.buscarAtivoPorId(id).orElseThrow(() -> new ClienteNaoEncontradoException(id));
    }
}
