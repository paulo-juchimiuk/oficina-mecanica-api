package br.com.oficinamecanica.autenticacao.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.util.UUID;

public record Usuario(UUID id, String login, String senhaHash, String perfil) {

    private static final String CODIGO = "USUARIO_INVALIDO";

    public Usuario {
        exigirPreenchido(id, "Usuario exige identidade");
        exigirPreenchido(login, "Usuario exige login");
        exigirPreenchido(senhaHash, "Usuario exige senha");
        exigirPreenchido(perfil, "Usuario exige perfil");
    }

    private static void exigirPreenchido(Object valor, String mensagem) {
        if (valor == null || valor.toString().isBlank()) {
            throw new DadosInvalidosException(CODIGO, mensagem);
        }
    }
}
