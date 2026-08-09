package br.com.oficinamecanica.catalogo.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Dinheiro(BigDecimal valor, String moeda) {

    private static final String CODIGO = "VALOR_MONETARIO_INVALIDO";
    private static final String MOEDA_UNICA = "BRL";
    private static final int CASAS_DECIMAIS = 2;
    private static final BigDecimal TETO = new BigDecimal("9999999999.99");

    public Dinheiro {
        if (valor == null) {
            throw new DadosInvalidosException(CODIGO, "Valor monetario e obrigatorio");
        }
        if (!MOEDA_UNICA.equals(moeda)) {
            throw new DadosInvalidosException(CODIGO, "Moeda deve ser " + MOEDA_UNICA);
        }
        if (valor.signum() < 0) {
            throw new DadosInvalidosException(CODIGO, "Valor monetario nao pode ser negativo");
        }
        if (valor.stripTrailingZeros().scale() > CASAS_DECIMAIS) {
            throw new DadosInvalidosException(CODIGO, "Valor monetario admite no maximo duas casas decimais");
        }
        if (valor.compareTo(TETO) > 0) {
            throw new DadosInvalidosException(CODIGO, "Valor monetario excede o teto de " + TETO);
        }
        valor = valor.setScale(CASAS_DECIMAIS, RoundingMode.UNNECESSARY);
    }
}
