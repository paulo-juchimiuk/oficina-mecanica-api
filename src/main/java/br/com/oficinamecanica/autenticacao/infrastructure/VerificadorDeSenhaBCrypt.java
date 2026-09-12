package br.com.oficinamecanica.autenticacao.infrastructure;

import br.com.oficinamecanica.autenticacao.application.VerificadorDeSenha;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class VerificadorDeSenhaBCrypt implements VerificadorDeSenha {

    private final PasswordEncoder encoder;

    VerificadorDeSenhaBCrypt(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public boolean confere(String senhaInformada, String senhaHash) {
        return encoder.matches(senhaInformada, senhaHash);
    }
}
