package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.ordemservico.domain.CodigoAcompanhamento;
import br.com.oficinamecanica.ordemservico.domain.Orcamento;

public interface EnvioDeOrcamento {

    void enviar(String email, CodigoAcompanhamento codigoAcompanhamento, Orcamento versao);
}
