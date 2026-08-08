package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;

public record Contato(String email, String telefone) {

    private static final String CODIGO = "CONTATO_INVALIDO";

    public Contato {
        email = email == null ? null : email.trim();
        telefone = telefone == null || telefone.isBlank() ? null : telefone.trim();
        if (email == null || email.isEmpty()) {
            throw new DadosInvalidosException(CODIGO, "Contato exige e-mail, que e o destino do envio do Orcamento");
        }
    }
}
