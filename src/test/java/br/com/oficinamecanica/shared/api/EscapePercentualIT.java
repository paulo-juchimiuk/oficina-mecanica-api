package br.com.oficinamecanica.shared.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Escape percentual invalido responde 400, com o servidor real decodificando a requisicao")
class EscapePercentualIT extends IntegracaoBase {

    @LocalServerPort
    private int porta;

    private String respostaBruta(String requisicao) throws Exception {
        try (Socket socket = new Socket("localhost", porta)) {
            OutputStream saida = socket.getOutputStream();
            saida.write(requisicao.getBytes(StandardCharsets.US_ASCII));
            saida.flush();
            try (BufferedReader leitor =
                         new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                return leitor.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    @Test
    @DisplayName("deve responder 400 no corpo urlencoded malformado da rota publica de login, e nunca 500")
    void deveResponder400NoCorpoDaRotaPublica() throws Exception {
        HttpResponse<String> resposta = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta + PREFIXO + "/auth/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("n=%zz"))
                .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(resposta.statusCode()).isEqualTo(400);
        assertThat(resposta.body()).contains("REQUISICAO_INVALIDA");
    }

    @Test
    @DisplayName("deve responder 400 na query string malformada, e nunca 500")
    void deveResponder400NaQueryString() throws Exception {
        String token = tokenAdministrativo();

        String resposta = respostaBruta("""
                GET %s/clientes?documento=%%zz HTTP/1.1\r
                Host: localhost:%d\r
                Authorization: Bearer %s\r
                Connection: close\r
                \r
                """.formatted(PREFIXO, porta, token));

        assertThat(resposta).startsWith("HTTP/1.1 400");
        assertThat(resposta).contains("REQUISICAO_INVALIDA");
    }
}
