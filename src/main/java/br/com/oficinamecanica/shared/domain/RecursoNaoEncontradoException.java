package br.com.oficinamecanica.shared.domain;

public class RecursoNaoEncontradoException extends DominioException {

    public RecursoNaoEncontradoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
