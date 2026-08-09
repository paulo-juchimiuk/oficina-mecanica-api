package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Clientes;
import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoNaoEncontradaException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ConcluirDiagnosticoUseCase {

    private final OrdemServicoRepository ordensServico;
    private final Clientes clientes;
    private final EnvioDeOrcamento envioDeOrcamento;

    public ConcluirDiagnosticoUseCase(OrdemServicoRepository ordensServico, Clientes clientes,
                                      EnvioDeOrcamento envioDeOrcamento) {
        this.ordensServico = ordensServico;
        this.clientes = clientes;
        this.envioDeOrcamento = envioDeOrcamento;
    }

    @Transactional
    public OrdemServico executar(UUID id) {
        OrdemServico ordemServico = ordensServico.buscarPorId(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
        Orcamento versaoEnviada = ordemServico.concluirDiagnostico();
        OrdemServico gravada = ordensServico.salvar(ordemServico);
        enviarAoCliente(gravada, versaoEnviada);
        return gravada;
    }

    private void enviarAoCliente(OrdemServico ordemServico, Orcamento versao) {
        String email = clientes.emailDe(ordemServico.clienteId())
                .orElseThrow(() -> new ClienteNaoEncontradoException(ordemServico.clienteId().toString()));
        envioDeOrcamento.enviar(email, ordemServico.codigoAcompanhamento(), versao);
    }
}
