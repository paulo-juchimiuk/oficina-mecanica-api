package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class OrcamentoJaEnviadoException extends ConflitoDeEstadoException {

    public OrcamentoJaEnviadoException(int versao) {
        super("ORCAMENTO_JA_ENVIADO",
                "A versao " + versao + " do Orcamento ja foi enviada e e imutavel");
    }
}
