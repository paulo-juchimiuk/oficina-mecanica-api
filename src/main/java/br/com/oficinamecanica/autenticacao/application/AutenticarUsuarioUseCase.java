package br.com.oficinamecanica.autenticacao.application;

import br.com.oficinamecanica.autenticacao.domain.CredenciaisInvalidasException;
import br.com.oficinamecanica.autenticacao.domain.Usuario;
import br.com.oficinamecanica.autenticacao.domain.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticarUsuarioUseCase {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final EmissorDeToken emissorDeToken;

    public AutenticarUsuarioUseCase(UsuarioRepository usuarios, PasswordEncoder encoder, EmissorDeToken emissorDeToken) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.emissorDeToken = emissorDeToken;
    }

    public TokenJwt executar(String login, String senha) {
        Usuario usuario = usuarios.buscarPorLogin(login).orElseThrow(CredenciaisInvalidasException::new);
        if (!encoder.matches(senha, usuario.senhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        return emissorDeToken.emitir(usuario);
    }
}
