package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

public record CodigoAcompanhamento(String valor) {

    private static final String CODIGO = "CODIGO_ACOMPANHAMENTO_INVALIDO";
    private static final String PREFIXO = "ACMP-";
    private static final int BYTES_ALEATORIOS = 16;
    private static final Pattern FORMATO = Pattern.compile("^" + PREFIXO + "[0-9a-f]{32}$");
    private static final SecureRandom FONTE_SEGURA = new SecureRandom();

    public CodigoAcompanhamento {
        if (valor == null || !FORMATO.matcher(valor).matches()) {
            throw new DadosInvalidosException(CODIGO, "Codigo de acompanhamento fora do formato esperado");
        }
    }

    public static Optional<CodigoAcompanhamento> de(String valor) {
        if (valor == null || !FORMATO.matcher(valor).matches()) {
            return Optional.empty();
        }
        return Optional.of(new CodigoAcompanhamento(valor));
    }

    public static CodigoAcompanhamento gerar() {
        byte[] aleatorios = new byte[BYTES_ALEATORIOS];
        FONTE_SEGURA.nextBytes(aleatorios);
        return new CodigoAcompanhamento(PREFIXO + HexFormat.of().formatHex(aleatorios));
    }
}
