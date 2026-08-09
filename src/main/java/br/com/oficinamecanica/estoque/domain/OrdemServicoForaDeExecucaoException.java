package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import java.util.UUID;

public class OrdemServicoForaDeExecucaoException extends ConflitoDeEstadoException {

    public OrdemServicoForaDeExecucaoException(UUID id) {
        super("ORDEM_SERVICO_FORA_DE_EXECUCAO",
                "Ordem de Servico " + id + " nao esta EM_EXECUCAO");
    }
}
