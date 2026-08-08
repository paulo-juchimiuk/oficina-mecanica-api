package br.com.oficinamecanica.shared.domain;

public class ConflitoDeEstadoException extends DominioException {

    public ConflitoDeEstadoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
