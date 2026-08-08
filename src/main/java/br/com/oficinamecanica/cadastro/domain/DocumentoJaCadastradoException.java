package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;

public class DocumentoJaCadastradoException extends ConflitoDeEstadoException {

    public DocumentoJaCadastradoException() {
        super("DOCUMENTO_JA_CADASTRADO", "Documento ja pertence a outro cliente, inclusive um removido logicamente");
    }
}
