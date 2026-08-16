package br.com.oficinamecanica.shared.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Content-Type recusado responde 415, com o servidor real lendo o cabecalho")
class TipoDeConteudoIT extends IntegracaoBase {

    @LocalServerPort
    private int porta;

    private HttpResponse<String> enviar(String metodo, String caminho, String tipoDeConteudo) throws Exception {
        HttpRequest.BodyPublisher corpo = metodo.equals("GET")
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString("{}");
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", tipoDeConteudo)
                .method(metodo, corpo)
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"*/*", "text/*", "application/*", "*", "application/*+json"})
    @DisplayName("deve responder 415 para Content-Type com curinga na rota publica de login, e nunca 500")
    void deveResponder415ParaCuringa(String tipoDeConteudo) throws Exception {
        HttpResponse<String> resposta = enviar("POST", PREFIXO + "/auth/login", tipoDeConteudo);

        assertThat(resposta.statusCode()).isEqualTo(415);
        assertThat(resposta.body()).contains("REQUISICAO_INVALIDA");
    }

    @Test
    @DisplayName("deve responder 415 para multipart na rota que consome corpo, e nunca 500")
    void deveResponder415ParaMultipartNaRotaComCorpo() throws Exception {
        HttpResponse<String> resposta = enviar("POST", PREFIXO + "/auth/login", "multipart/form-data");

        assertThat(resposta.statusCode()).isEqualTo(415);
        assertThat(resposta.body()).contains("REQUISICAO_INVALIDA");
    }

    @Test
    @DisplayName("deve ignorar o Content-Type multipart no GET, que nao consome corpo, e responder 401 sem token")
    void deveIgnorarMultipartNoGet() throws Exception {
        HttpResponse<String> resposta = enviar("GET", PREFIXO + "/clientes", "multipart/form-data");

        assertThat(resposta.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("deve manter o caminho feliz com application/json")
    void deveManterOCaminhoFeliz() throws Exception {
        criarUsuarioDeTeste();

        HttpResponse<String> resposta = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta + PREFIXO + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"login": "%s", "senha": "%s"}
                        """.formatted(LOGIN_DE_TESTE, SENHA_DE_TESTE)))
                .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).contains("token");
    }
}
