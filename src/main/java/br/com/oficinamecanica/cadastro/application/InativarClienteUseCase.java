package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import br.com.oficinamecanica.cadastro.domain.ClienteComOrdemServicoEmAndamentoException;
import br.com.oficinamecanica.cadastro.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.cadastro.domain.ClienteRepository;
import br.com.oficinamecanica.cadastro.domain.OrdensServicoEmAndamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class InativarClienteUseCase {

    private final ClienteRepository clientes;
    private final OrdensServicoEmAndamento ordensServico;

    public InativarClienteUseCase(ClienteRepository clientes, OrdensServicoEmAndamento ordensServico) {
        this.clientes = clientes;
        this.ordensServico = ordensServico;
    }

    @Transactional
    public void executar(UUID id) {
        Cliente cliente = clientes.buscarAtivoPorId(id).orElseThrow(() -> new ClienteNaoEncontradoException(id));
        if (ordensServico.existemParaCliente(id)) {
            throw new ClienteComOrdemServicoEmAndamentoException();
        }
        cliente.inativar();
        clientes.salvar(cliente);
    }
}
