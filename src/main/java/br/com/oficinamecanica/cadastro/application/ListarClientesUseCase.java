package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.Documento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class ListarClientesUseCase {

    private final ClienteRepository clientes;

    public ListarClientesUseCase(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Transactional(readOnly = true)
    public List<Cliente> executar(Optional<Documento> documento) {
        if (documento.isEmpty()) {
            return clientes.listarAtivos();
        }
        return clientes.buscarAtivoPorDocumento(documento.get()).map(List::of).orElseGet(List::of);
    }
}
