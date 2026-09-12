package br.com.oficinamecanica.autenticacao.application;

import br.com.oficinamecanica.autenticacao.domain.CredenciaisInvalidasException;
import br.com.oficinamecanica.autenticacao.domain.Usuario;
import br.com.oficinamecanica.autenticacao.domain.UsuarioRepository;

public class AutenticarUsuarioUseCase {

    private final UsuarioRepository usuarios;
    private final VerificadorDeSenha verificadorDeSenha;
    private final EmissorDeToken emissorDeToken;

    public AutenticarUsuarioUseCase(UsuarioRepository usuarios, VerificadorDeSenha verificadorDeSenha, EmissorDeToken emissorDeToken) {
        this.usuarios = usuarios;
        this.verificadorDeSenha = verificadorDeSenha;
        this.emissorDeToken = emissorDeToken;
    }

    public TokenJwt executar(String login, String senha) {
        Usuario usuario = usuarios.buscarPorLogin(login).orElseThrow(CredenciaisInvalidasException::new);
        if (!verificadorDeSenha.confere(senha, usuario.senhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        return emissorDeToken.emitir(usuario);
    }
}
