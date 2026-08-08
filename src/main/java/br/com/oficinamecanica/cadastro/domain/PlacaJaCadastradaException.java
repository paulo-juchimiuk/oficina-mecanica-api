package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class PlacaJaCadastradaException extends ConflitoDeEstadoException {

    public PlacaJaCadastradaException() {
        super("PLACA_JA_CADASTRADA", "Placa ja pertence a outro veiculo, inclusive um removido logicamente");
    }
}
