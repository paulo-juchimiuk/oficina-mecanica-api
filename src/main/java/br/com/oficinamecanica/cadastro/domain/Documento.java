package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;

public record Documento(String numero) {

    private static final String CODIGO = "DOCUMENTO_INVALIDO";
    private static final String FORMATO = "\\d{11}|\\d{14}";
    private static final int DIGITOS_CPF = 11;
    private static final int[] PESOS_CPF_PRIMEIRO = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CPF_SEGUNDO = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_PRIMEIRO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_SEGUNDO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int RESTO_SEM_DIGITO = 2;
    private static final int MODULO = 11;

    public Documento {
        exigirDocumentoValido(numero);
    }

    private static void exigirDocumentoValido(String numero) {
        if (numero == null || !numero.matches(FORMATO)) {
            throw new DadosInvalidosException(CODIGO, "Documento deve ter 11 digitos (CPF) ou 14 (CNPJ), somente numeros");
        }
        if (temDigitoUnico(numero)) {
            throw new DadosInvalidosException(CODIGO, "Documento com todos os digitos iguais nao e valido");
        }
        if (numero.length() == DIGITOS_CPF) {
            exigirDigitosVerificadores(numero, PESOS_CPF_PRIMEIRO, PESOS_CPF_SEGUNDO, "CPF");
            return;
        }
        exigirDigitosVerificadores(numero, PESOS_CNPJ_PRIMEIRO, PESOS_CNPJ_SEGUNDO, "CNPJ");
    }

    private static boolean temDigitoUnico(String numero) {
        return numero.chars().distinct().count() == 1;
    }

    private static void exigirDigitosVerificadores(String numero, int[] pesosPrimeiro, int[] pesosSegundo, String tipo) {
        int posicaoDoPrimeiro = pesosPrimeiro.length;
        int primeiroInformado = valorDoDigito(numero, posicaoDoPrimeiro);
        int segundoInformado = valorDoDigito(numero, posicaoDoPrimeiro + 1);
        if (primeiroInformado != digitoVerificador(numero, pesosPrimeiro)
                || segundoInformado != digitoVerificador(numero, pesosSegundo)) {
            throw new DadosInvalidosException(CODIGO, tipo + " com digito verificador invalido");
        }
    }

    private static int digitoVerificador(String numero, int[] pesos) {
        int soma = 0;
        for (int posicao = 0; posicao < pesos.length; posicao++) {
            soma += valorDoDigito(numero, posicao) * pesos[posicao];
        }
        int resto = soma % MODULO;
        if (resto < RESTO_SEM_DIGITO) {
            return 0;
        }
        return MODULO - resto;
    }

    private static int valorDoDigito(String numero, int posicao) {
        return numero.charAt(posicao) - '0';
    }
}
