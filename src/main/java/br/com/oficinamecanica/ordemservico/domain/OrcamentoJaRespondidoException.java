package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class OrcamentoJaRespondidoException extends ConflitoDeEstadoException {

    public OrcamentoJaRespondidoException(int versao, SituacaoOrcamento situacao) {
        super("ORCAMENTO_JA_RESPONDIDO",
                "A versao " + versao + " do Orcamento ja esta " + situacao);
    }
}
