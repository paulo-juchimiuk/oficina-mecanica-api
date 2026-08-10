package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Codigo de acompanhamento")
class CodigoAcompanhamentoTest {

    private static final int CODIGOS_GERADOS = 2000;
    private static final int DIGITOS_HEXADECIMAIS = 32;
    private static final String PREFIXO = "ACMP-";

    @Test
    @DisplayName("deve gerar o codigo com 32 digitos hexadecimais, que sao os 128 bits que o ADR-007 exige")
    void deveGerarCodigoComCentoEVinteEOitoBits() {
        String valor = CodigoAcompanhamento.gerar().valor();
        assertThat(valor).startsWith(PREFIXO);
        assertThat(valor.substring(PREFIXO.length()))
                .hasSize(DIGITOS_HEXADECIMAIS)
                .matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("deve variar TODAS as 32 posicoes, o que um identificador com bits fixos de versao nao faz")
    void deveVariarTodasAsPosicoes() {
        List<String> gerados = IntStream.range(0, CODIGOS_GERADOS)
                .mapToObj(vez -> CodigoAcompanhamento.gerar().valor().substring(PREFIXO.length()))
                .toList();

        for (int posicao = 0; posicao < DIGITOS_HEXADECIMAIS; posicao++) {
            int atual = posicao;
            assertThat(gerados.stream().map(codigo -> codigo.charAt(atual)).distinct().count())
                    .as("digitos distintos observados na posicao %d", atual)
                    .isGreaterThan(1);
        }
    }

    @Test
    @DisplayName("deve gerar codigos distintos a cada chamada")
    void deveGerarCodigosDistintos() {
        Set<String> gerados = new HashSet<>();
        IntStream.range(0, CODIGOS_GERADOS).forEach(vez -> gerados.add(CodigoAcompanhamento.gerar().valor()));
        assertThat(gerados).hasSize(CODIGOS_GERADOS);
    }

    @Test
    @DisplayName("deve comparar por valor, porque o codigo e um objeto de valor")
    void deveCompararPorValor() {
        String valor = CodigoAcompanhamento.gerar().valor();
        assertThat(new CodigoAcompanhamento(valor)).isEqualTo(new CodigoAcompanhamento(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "ACMP-", "8731a4acc01cc83d57ec255987403be8", "ACMP-8731a4acc01cc83d57ec255987403be",
            "ACMP-8731A4ACC01CC83D57EC255987403BE8", "ACMP-8731a4acc01cc83d57ec255987403be8f", "OUTRO-prefixo"})
    @DisplayName("deve recusar codigo fora do formato")
    void deveRecusarCodigoForaDoFormato(String invalido) {
        assertThatThrownBy(() -> new CodigoAcompanhamento(invalido))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar codigo nulo")
    void deveRecusarCodigoNulo() {
        assertThatThrownBy(() -> new CodigoAcompanhamento(null))
                .isInstanceOf(DadosInvalidosException.class);
    }
}
