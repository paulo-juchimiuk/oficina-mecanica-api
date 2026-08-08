package br.com.oficinamecanica.shared.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Hierarquia de erros do dominio")
class ErrosDoDominioTest {

    @Test
    @DisplayName("deve carregar codigo e mensagem no erro de dados invalidos, que a API devolve como 400")
    void deveCarregarDadosInvalidos() {
        DominioException erro = new DadosInvalidosException("DOCUMENTO_INVALIDO", "digito verificador invalido");
        assertThat(erro.codigo()).isEqualTo("DOCUMENTO_INVALIDO");
        assertThat(erro.getMessage()).isEqualTo("digito verificador invalido");
    }

    @Test
    @DisplayName("deve carregar codigo e mensagem no erro de recurso nao encontrado, que a API devolve como 404")
    void deveCarregarNaoEncontrado() {
        DominioException erro = new RecursoNaoEncontradoException("CLIENTE_NAO_ENCONTRADO", "nao encontrado");
        assertThat(erro.codigo()).isEqualTo("CLIENTE_NAO_ENCONTRADO");
        assertThat(erro.getMessage()).isEqualTo("nao encontrado");
    }

    @Test
    @DisplayName("deve carregar codigo e mensagem no erro de conflito, que a API devolve como 409")
    void deveCarregarConflito() {
        DominioException erro = new ConflitoDeEstadoException("PLACA_JA_CADASTRADA", "placa em uso");
        assertThat(erro.codigo()).isEqualTo("PLACA_JA_CADASTRADA");
        assertThat(erro.getMessage()).isEqualTo("placa em uso");
    }
}
