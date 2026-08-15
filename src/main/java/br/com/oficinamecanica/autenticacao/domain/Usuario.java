package br.com.oficinamecanica.autenticacao.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.util.UUID;

public class Usuario {

    private static final String CODIGO = "USUARIO_INVALIDO";

    private final UUID id;
    private final String login;
    private final String senhaHash;
    private final String perfil;

    public Usuario(UUID id, String login, String senhaHash, String perfil) {
        exigirPreenchido(id, "Usuario exige identidade");
        exigirPreenchido(login, "Usuario exige login");
        exigirPreenchido(senhaHash, "Usuario exige senha");
        exigirPreenchido(perfil, "Usuario exige perfil");
        this.id = id;
        this.login = login;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    public String login() {
        return login;
    }

    public String senhaHash() {
        return senhaHash;
    }

    public String perfil() {
        return perfil;
    }

    private static void exigirPreenchido(Object valor, String mensagem) {
        if (valor == null || valor.toString().isBlank()) {
            throw new DadosInvalidosException(CODIGO, mensagem);
        }
    }
}
