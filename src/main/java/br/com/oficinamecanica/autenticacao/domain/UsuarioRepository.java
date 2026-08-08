package br.com.oficinamecanica.autenticacao.domain;

import java.util.Optional;

public interface UsuarioRepository {

    Optional<Usuario> buscarPorLogin(String login);
}
