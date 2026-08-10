package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class OrdemServicoSemOrcamentoException extends ConflitoDeEstadoException {

    public OrdemServicoSemOrcamentoException() {
        super("ORDEM_SERVICO_SEM_ORCAMENTO",
                "A Ordem de Servico nao tem itens, entao nao ha Orcamento a enviar");
    }
}
