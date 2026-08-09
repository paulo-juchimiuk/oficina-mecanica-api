package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import java.util.UUID;

public class OrdemServicoSemOrcamentoException extends ConflitoDeEstadoException {

    public OrdemServicoSemOrcamentoException(UUID ordemServicoId) {
        super("ORDEM_SERVICO_SEM_ORCAMENTO",
                "A Ordem de Servico " + ordemServicoId + " nao tem itens, entao nao ha Orcamento a enviar");
    }
}
