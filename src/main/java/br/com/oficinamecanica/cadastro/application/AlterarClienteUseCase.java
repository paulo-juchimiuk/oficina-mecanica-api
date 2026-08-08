package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import br.com.oficinamecanica.cadastro.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.Contato;
import br.com.oficinamecanica.cadastro.domain.Documento;
import br.com.oficinamecanica.cadastro.domain.DocumentoJaCadastradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AlterarClienteUseCase {

    private final ClienteRepository clientes;

    public AlterarClienteUseCase(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Transactional
    public Cliente executar(UUID id, String nome, Documento documento, Contato contato) {
        Cliente cliente = clientes.buscarAtivoPorId(id).orElseThrow(() -> new ClienteNaoEncontradoException(id));
        if (clientes.documentoJaCadastradoPorOutro(id, documento)) {
            throw new DocumentoJaCadastradoException();
        }
        cliente.alterar(nome, documento, contato);
        return clientes.salvar(cliente);
    }
}
