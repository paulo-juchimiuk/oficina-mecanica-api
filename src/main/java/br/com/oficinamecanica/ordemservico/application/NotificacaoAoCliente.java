package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.CodigoAcompanhamento;
import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;

public interface NotificacaoAoCliente {

    void enviarOrcamento(String email, CodigoAcompanhamento codigoAcompanhamento, Orcamento versao,
                         StatusOrdemServico status);

    void enviarMudancaDeStatus(String email, CodigoAcompanhamento codigoAcompanhamento, StatusOrdemServico status);
}
