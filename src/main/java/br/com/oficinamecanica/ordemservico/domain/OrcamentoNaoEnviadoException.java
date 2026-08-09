package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class OrcamentoNaoEnviadoException extends ConflitoDeEstadoException {

    public OrcamentoNaoEnviadoException(int versao) {
        super("ORCAMENTO_NAO_ENVIADO",
                "A versao " + versao + " do Orcamento ainda nao foi enviada ao Cliente");
    }
}
