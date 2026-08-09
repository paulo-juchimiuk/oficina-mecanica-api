package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;
import java.util.UUID;

public class VeiculoNaoEncontradoException extends RecursoNaoEncontradoException {

    public VeiculoNaoEncontradoException(UUID id) {
        super("VEICULO_NAO_ENCONTRADO", "Veiculo " + id + " nao encontrado");
    }
}
