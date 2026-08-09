package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import java.util.UUID;

public class VeiculoDeOutroClienteException extends ConflitoDeEstadoException {

    public VeiculoDeOutroClienteException(UUID veiculoId) {
        super("VEICULO_DE_OUTRO_CLIENTE",
                "O Veiculo " + veiculoId + " nao pertence ao Cliente informado");
    }
}
