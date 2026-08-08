package br.com.oficinamecanica.autenticacao.domain;

import br.com.oficinamecanica.shared.domain.DominioException;

public class CredenciaisInvalidasException extends DominioException {

    public CredenciaisInvalidasException() {
        super("CREDENCIAIS_INVALIDAS", "Login ou senha invalidos");
    }
}
