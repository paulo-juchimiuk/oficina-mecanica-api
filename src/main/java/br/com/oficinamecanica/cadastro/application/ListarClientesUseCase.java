package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.Documento;
import java.util.List;
import java.util.Optional;

public class ListarClientesUseCase {

    private final ClienteRepository clientes;

    public ListarClientesUseCase(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    public List<Cliente> executar(Optional<Documento> documento) {
        if (documento.isEmpty()) {
            return clientes.listarAtivos();
        }
        return clientes.buscarAtivoPorDocumento(documento.get()).map(List::of).orElseGet(List::of);
    }
}
