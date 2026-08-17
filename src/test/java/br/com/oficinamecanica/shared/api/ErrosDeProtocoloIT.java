package br.com.oficinamecanica.shared.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Erros de protocolo respondem no envelope do contrato")
class ErrosDeProtocoloIT extends IntegracaoBase {

    private String token;

    @BeforeEach
    void prepararCenario() throws Exception {
        token = tokenAdministrativo();
    }

    @Test
    @DisplayName("deve responder 405 com envelope quando o metodo nao e suportado na rota, e nunca 500")
    void deveResponder405ComEnvelope() throws Exception {
        mockMvc.perform(delete(PREFIXO + "/auth/login").header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 400 com envelope quando o corpo esta malformado, e nunca 500")
    void deveResponder400ComCorpoMalformado() throws Exception {
        mockMvc.perform(post(PREFIXO + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 404 com envelope em rota inexistente, e nunca 500")
    void deveResponder404EmRotaInexistente() throws Exception {
        mockMvc.perform(get(PREFIXO + "/nao-existe").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve responder 404 com envelope quando a rota implementada leva barra no fim")
    void deveResponder404ComBarraNoFim() throws Exception {
        mockMvc.perform(get(PREFIXO + "/clientes/").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve responder 406 com envelope quando o Accept nao aceita JSON")
    void deveResponder406ComEnvelope() throws Exception {
        mockMvc.perform(get(PREFIXO + "/clientes")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder credencial invalida no envelope, e nunca pagina de erro do framework")
    void deveResponderCredencialInvalidaNoEnvelope() throws Exception {
        mockMvc.perform(post(PREFIXO + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_HTML)
                        .content("""
                                {"login": "%s", "senha": "senha-errada"}
                                """.formatted(LOGIN_DE_TESTE)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").isNotEmpty());
    }

    @Test
    @DisplayName("deve responder 400 quando o identificador do caminho decodifica para branco")
    void deveResponder400ParaIdentificadorEmBranco() throws Exception {
        mockMvc.perform(get(PREFIXO + "/clientes/{id}", " ").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve recusar identificador mais curto que o UUID, em vez de completar com zeros")
    void deveRecusarIdentificadorIncompleto() throws Exception {
        mockMvc.perform(get(PREFIXO + "/clientes/f0000000-0000-4000-8000-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve recusar identificador com o tamanho certo e os grupos errados, que resolveria para outro recurso")
    void deveRecusarIdentificadorMalformadoDoMesmoTamanho() throws Exception {
        mockMvc.perform(get(PREFIXO + "/clientes/0c0000000-000-4000-8000-000000000001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve aceitar identificador em caixa alta, que e forma legitima do UUID")
    void deveAceitarIdentificadorEmCaixaAlta() throws Exception {
        mockMvc.perform(get(PREFIXO + "/clientes/C0000000-0000-4000-8000-000000000001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve recusar identificador mais longo que o UUID")
    void deveRecusarIdentificadorLongo() throws Exception {
        mockMvc.perform(get(PREFIXO + "/clientes/f0000000-0000-4000-8000-0000000000001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 406 com envelope tambem na rota publica de login")
    void deveResponder406NaRotaPublica() throws Exception {
        mockMvc.perform(post(PREFIXO + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_XML)
                        .content("""
                                {"login": "%s", "senha": "%s"}
                                """.formatted(LOGIN_DE_TESTE, SENHA_DE_TESTE)))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }
}
