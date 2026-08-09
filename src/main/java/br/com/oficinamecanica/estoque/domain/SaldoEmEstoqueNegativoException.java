package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import java.util.UUID;

public class SaldoEmEstoqueNegativoException extends ConflitoDeEstadoException {

    public SaldoEmEstoqueNegativoException(UUID pecaId) {
        super("SALDO_EM_ESTOQUE_NEGATIVO",
                "Saldo em estoque da Peca " + pecaId + " nao pode ficar negativo");
    }
}
