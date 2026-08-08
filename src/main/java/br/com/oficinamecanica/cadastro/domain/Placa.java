package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.util.Locale;

public record Placa(String valor) {

    private static final String CODIGO = "PLACA_INVALIDA";
    private static final String FORMATO_ANTIGO = "[A-Z]{3}[0-9]{4}";
    private static final String FORMATO_MERCOSUL = "[A-Z]{3}[0-9][A-Z][0-9]{2}";

    public Placa {
        if (valor == null) {
            throw new DadosInvalidosException(CODIGO, "Placa e obrigatoria");
        }
        valor = valor.trim().toUpperCase(Locale.ROOT);
        if (!valor.matches(FORMATO_ANTIGO) && !valor.matches(FORMATO_MERCOSUL)) {
            throw new DadosInvalidosException(CODIGO, "Placa deve seguir o formato antigo AAA9999 ou o Mercosul AAA9A99, sem hifen");
        }
    }
}
