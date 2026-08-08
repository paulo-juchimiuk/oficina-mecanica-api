package br.com.oficinamecanica.autenticacao.application;

import br.com.oficinamecanica.autenticacao.domain.Usuario;

public interface EmissorDeToken {

    TokenJwt emitir(Usuario usuario);
}
