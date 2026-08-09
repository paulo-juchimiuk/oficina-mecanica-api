package br.com.oficinamecanica.autenticacao.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.yaml.snakeyaml.Yaml;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Autenticacao das APIs administrativas")
class AutenticacaoIT extends IntegracaoBase {

    @Value("${oficina.jwt.segredo}")
    private String segredoDaAplicacao;

    @Test
    @DisplayName("deve emitir JWT com credenciais validas")
    void deveEmitirTokenComCredenciaisValidas() throws Exception {
        criarUsuarioDeTeste();

        mockMvc.perform(post(PREFIXO + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "%s", "senha": "%s"}
                                """.formatted(LOGIN_DE_TESTE, SENHA_DE_TESTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiraEm").isNotEmpty());
    }

    @Test
    @DisplayName("deve recusar senha errada com 401, e o corpo nao revela qual campo falhou")
    void deveRecusarSenhaErrada() throws Exception {
        criarUsuarioDeTeste();

        mockMvc.perform(post(PREFIXO + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "%s", "senha": "errada"}
                                """.formatted(LOGIN_DE_TESTE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.mensagem").value("Login ou senha invalidos"));
    }

    @Test
    @DisplayName("deve recusar login inexistente com a mesma resposta da senha errada")
    void deveRecusarLoginInexistente() throws Exception {
        mockMvc.perform(post(PREFIXO + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "fantasma", "senha": "qualquer"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    @DisplayName("deve recusar login sem senha com 400")
    void deveRecusarLoginSemSenha() throws Exception {
        mockMvc.perform(post(PREFIXO + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "admin"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve recusar rota administrativa sem JWT, com corpo de erro em JSON")
    void deveRecusarRotaAdministrativaSemToken() throws Exception {
        mockMvc.perform(get(PREFIXO + "/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"));
    }

    @Test
    @DisplayName("deve recusar JWT bem formado com assinatura de outro segredo")
    void deveRecusarTokenComAssinaturaErrada() throws Exception {
        String forjado = tokenAssinadoCom("outro-segredo-de-32-bytes-ou-mais-para-o-teste", Instant.now().plusSeconds(600));

        mockMvc.perform(get(PREFIXO + "/clientes").header("Authorization", "Bearer " + forjado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"));
    }

    @Test
    @DisplayName("deve recusar JWT expirado, assinado com o segredo real")
    void deveRecusarTokenExpirado() throws Exception {
        String expirado = tokenAssinadoCom(segredoDaAplicacao, Instant.now().minusSeconds(3600));

        mockMvc.perform(get(PREFIXO + "/clientes").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"));
    }

    private String tokenAssinadoCom(String segredo, Instant expiraEm) {
        SecretKeySpec chave = new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtClaimsSet reivindicacoes = JwtClaimsSet.builder()
                .issuer("oficina-mecanica-api")
                .subject(LOGIN_DE_TESTE)
                .issuedAt(expiraEm.minusSeconds(600))
                .expiresAt(expiraEm)
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), reivindicacoes))
                .getTokenValue();
    }

    @Test
    @DisplayName("deve responder 415 no login com Content-Type nao suportado, e nao 401, porque a rota e publica")
    void deveResponder415NoLoginComContentTypeErrado() throws Exception {
        mockMvc.perform(post(PREFIXO + "/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve liberar a especificacao gerada e declarar nela o esquema de seguranca, com o login isento")
    void deveLiberarEspecificacaoComEsquemaDeSeguranca() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.jwtAdministrativo.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].jwtAdministrativo").exists())
                .andExpect(jsonPath("$.paths['" + PREFIXO + "/auth/login'].post.security").isEmpty());
    }

    @Test
    @DisplayName("deve identificar a especificacao gerada com o mesmo titulo e versao do contrato versionado")
    void deveIdentificarEspecificacaoComOsDadosDoContrato() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value(doContrato("title")))
                .andExpect(jsonPath("$.info.version").value(doContrato("version")));
    }

    private String doContrato(String chave) throws IOException {
        try (InputStream contrato = Files.newInputStream(Path.of("openapi.yaml"))) {
            Map<String, Map<String, Object>> raiz = new Yaml().load(contrato);
            return String.valueOf(raiz.get("info").get(chave));
        }
    }
}
