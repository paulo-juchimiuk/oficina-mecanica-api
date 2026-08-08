package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Documento (CPF ou CNPJ)")
class DocumentoTest {

    @ParameterizedTest
    @ValueSource(strings = {"10433218100", "96001338914", "08386379499", "02654235114"})
    @DisplayName("deve aceitar CPF com digitos verificadores validos")
    void deveAceitarCpfValido(String cpf) {
        assertThat(new Documento(cpf).numero()).isEqualTo(cpf);
    }

    @ParameterizedTest
    @ValueSource(strings = {"31034131000113", "64752553000183"})
    @DisplayName("deve aceitar CNPJ com digitos verificadores validos")
    void deveAceitarCnpjValido(String cnpj) {
        assertThat(new Documento(cnpj).numero()).isEqualTo(cnpj);
    }

    @Test
    @DisplayName("deve recusar CPF com primeiro digito verificador errado")
    void deveRecusarCpfComPrimeiroDigitoErrado() {
        assertThatThrownBy(() -> new Documento("10433218200"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    @DisplayName("deve recusar CPF com segundo digito verificador errado")
    void deveRecusarCpfComSegundoDigitoErrado() {
        assertThatThrownBy(() -> new Documento("10433218101")).isInstanceOf(DadosInvalidosException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "99999999999", "00000000000000", "11111111111111"})
    @DisplayName("deve recusar CPF e CNPJ com todos os digitos iguais, inclusive a sequencia de zeros do CNPJ, que passa no modulo 11")
    void deveRecusarDocumentoComDigitoUnico(String documento) {
        assertThatThrownBy(() -> new Documento(documento))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("digitos iguais");
    }

    @Test
    @DisplayName("deve recusar CNPJ com digito verificador errado")
    void deveRecusarCnpjComDigitoErrado() {
        assertThatThrownBy(() -> new Documento("31034131000114"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("CNPJ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"104.332.181-00", "1043321810", "104332181000", "abcdefghijk", "", " "})
    @DisplayName("deve recusar o que nao tem 11 ou 14 digitos somente numericos")
    void deveRecusarFormatoInvalido(String valor) {
        assertThatThrownBy(() -> new Documento(valor))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("11 digitos");
    }

    @Test
    @DisplayName("deve recusar Documento nulo")
    void deveRecusarNulo() {
        assertThatThrownBy(() -> new Documento(null)).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve comparar por valor, como todo value object")
    void deveCompararPorValor() {
        assertThat(new Documento("10433218100")).isEqualTo(new Documento("10433218100"));
    }

    @Test
    @DisplayName("deve carregar o codigo de erro que a API devolve")
    void deveCarregarCodigoDeErro() {
        assertThatThrownBy(() -> new Documento("00000000000"))
                .isInstanceOf(DadosInvalidosException.class)
                .extracting(erro -> ((DadosInvalidosException) erro).codigo())
                .isEqualTo("DOCUMENTO_INVALIDO");
    }
}
