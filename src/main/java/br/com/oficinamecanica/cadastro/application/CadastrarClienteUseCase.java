package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.Contato;
import br.com.oficinamecanica.cadastro.domain.Documento;
import br.com.oficinamecanica.cadastro.domain.DocumentoJaCadastradoException;

public class CadastrarClienteUseCase {

    private final ClienteRepository clientes;

    public CadastrarClienteUseCase(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    public Cliente executar(String nome, Documento documento, Contato contato) {
        Cliente cliente = Cliente.cadastrar(nome, documento, contato);
        if (clientes.documentoJaCadastradoPorOutro(cliente.id(), documento)) {
            throw new DocumentoJaCadastradoException();
        }
        return clientes.salvar(cliente);
    }
}
