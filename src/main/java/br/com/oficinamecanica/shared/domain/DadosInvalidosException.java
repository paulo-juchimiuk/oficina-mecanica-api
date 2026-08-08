package br.com.oficinamecanica.shared.domain;

public class DadosInvalidosException extends DominioException {

    public DadosInvalidosException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
