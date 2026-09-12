package br.com.oficinamecanica.autenticacao.application;

public interface VerificadorDeSenha {

    boolean confere(String senhaInformada, String senhaHash);
}
