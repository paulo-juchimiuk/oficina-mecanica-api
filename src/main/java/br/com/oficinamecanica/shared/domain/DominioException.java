package br.com.oficinamecanica.shared.domain;

public abstract class DominioException extends RuntimeException {

    private final String codigo;

    protected DominioException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
