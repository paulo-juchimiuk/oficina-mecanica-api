package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import br.com.oficinamecanica.cadastro.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class DetalharClienteUseCase {

    private final ClienteRepository clientes;

    public DetalharClienteUseCase(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Transactional(readOnly = true)
    public Cliente executar(UUID id) {
        return clientes.buscarAtivoPorId(id).orElseThrow(() -> new ClienteNaoEncontradoException(id));
    }
}
