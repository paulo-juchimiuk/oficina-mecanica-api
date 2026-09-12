package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.ClienteNaoEncontradoException;
import br.com.oficinamecanica.ordemservico.domain.Clientes;
import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;

public class NotificadorDoCliente {

    private final Clientes clientes;
    private final NotificacaoAoCliente notificacao;

    public NotificadorDoCliente(Clientes clientes, NotificacaoAoCliente notificacao) {
        this.clientes = clientes;
        this.notificacao = notificacao;
    }

    public void notificarOrcamento(OrdemServico ordemServico, Orcamento versao) {
        notificacao.enviarOrcamento(emailDoCliente(ordemServico), ordemServico.codigoAcompanhamento(), versao);
    }

    public void notificarMudancaDeStatus(OrdemServico ordemServico) {
        notificacao.enviarMudancaDeStatus(
                emailDoCliente(ordemServico), ordemServico.codigoAcompanhamento(), ordemServico.status());
    }

    private String emailDoCliente(OrdemServico ordemServico) {
        return clientes.emailDe(ordemServico.clienteId())
                .orElseThrow(ClienteNaoEncontradoException::new);
    }
}
