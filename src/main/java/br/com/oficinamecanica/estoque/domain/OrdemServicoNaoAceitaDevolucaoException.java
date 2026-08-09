package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import java.util.UUID;

public class OrdemServicoNaoAceitaDevolucaoException extends ConflitoDeEstadoException {

    public OrdemServicoNaoAceitaDevolucaoException(UUID id) {
        super("ORDEM_SERVICO_NAO_ACEITA_DEVOLUCAO",
                "Ordem de Servico " + id + " nao esta EM_EXECUCAO, FINALIZADA ou ENTREGUE");
    }
}
