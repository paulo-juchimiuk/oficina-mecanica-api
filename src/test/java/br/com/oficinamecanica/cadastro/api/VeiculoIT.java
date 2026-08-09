package br.com.oficinamecanica.cadastro.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CRUD de veiculos")
class VeiculoIT extends IntegracaoBase {

    private String token;
    private String clienteId;

    @BeforeEach
    void prepararCenario() throws Exception {
        token = tokenAdministrativo();
        clienteId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?::uuid, ?, ?, ?)",
                clienteId, "Ana Beatriz Souza", "10433218100", "ana@example.com");
    }

    private String corpoDeVeiculo(String placa, String clienteId) {
        return """
                {"placa": "%s", "marca": "Fiat", "modelo": "Uno", "ano": 2015, "clienteId": "%s"}
                """.formatted(placa, clienteId);
    }

    private String cadastrar(String placa) throws Exception {
        String corpo = mockMvc.perform(post(PREFIXO + "/veiculos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeVeiculo(placa, clienteId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asText();
    }

    @Test
    @DisplayName("deve cadastrar veiculo com placa no formato antigo")
    void deveCadastrarPlacaAntiga() throws Exception {
        mockMvc.perform(post(PREFIXO + "/veiculos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeVeiculo("ABC1234", clienteId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("ABC1234"))
                .andExpect(jsonPath("$.clienteId").value(clienteId));
    }

    @Test
    @DisplayName("deve cadastrar veiculo com placa Mercosul e normalizar para caixa alta")
    void deveCadastrarPlacaMercosulNormalizada() throws Exception {
        mockMvc.perform(post(PREFIXO + "/veiculos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeVeiculo("rta2e19", clienteId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("RTA2E19"));
    }

    @Test
    @DisplayName("deve recusar placa fora dos dois formatos, com 400")
    void deveRecusarPlacaInvalida() throws Exception {
        mockMvc.perform(post(PREFIXO + "/veiculos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeVeiculo("ABC-1234", clienteId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve recusar ano fora da faixa de sanidade, com 400")
    void deveRecusarAnoForaDaFaixa() throws Exception {
        mockMvc.perform(post(PREFIXO + "/veiculos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"placa": "ABC1234", "marca": "Fiat", "modelo": "Uno", "ano": 1800, "clienteId": "%s"}
                                """.formatted(clienteId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("deve recusar vinculo com Cliente inexistente, com 404")
    void deveRecusarClienteInexistente() throws Exception {
        mockMvc.perform(post(PREFIXO + "/veiculos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeVeiculo("ABC1234", UUID.randomUUID().toString())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve recusar placa ja cadastrada, com 409")
    void deveRecusarPlacaDuplicada() throws Exception {
        cadastrar("ABC1234");

        mockMvc.perform(post(PREFIXO + "/veiculos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeVeiculo("abc1234", clienteId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PLACA_JA_CADASTRADA"));
    }

    @Test
    @DisplayName("deve listar veiculos ativos e filtrar por placa")
    void deveListarEFiltrar() throws Exception {
        cadastrar("ABC1234");
        cadastrar("RTA2E19");

        mockMvc.perform(get(PREFIXO + "/veiculos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get(PREFIXO + "/veiculos").param("placa", "abc1234")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placa").value("ABC1234"));
    }

    @Test
    @DisplayName("deve transferir o veiculo para outro Cliente")
    void deveTransferirProprietario() throws Exception {
        String veiculoId = cadastrar("ABC1234");
        String novoDono = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?::uuid, ?, ?, ?)",
                novoDono, "Carlos Eduardo Lima", "96001338914", "carlos@example.com");

        mockMvc.perform(put(PREFIXO + "/veiculos/" + veiculoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeVeiculo("ABC1234", novoDono)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(veiculoId))
                .andExpect(jsonPath("$.clienteId").value(novoDono));
    }

    @Test
    @DisplayName("deve remover logicamente e manter a placa reservada")
    void deveRemoverLogicamenteEReservarPlaca() throws Exception {
        String veiculoId = cadastrar("ABC1234");

        mockMvc.perform(delete(PREFIXO + "/veiculos/" + veiculoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PREFIXO + "/veiculos/" + veiculoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("VEICULO_NAO_ENCONTRADO"));

        Integer inativos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM veiculo WHERE id = ?::uuid AND ativo = FALSE", Integer.class, veiculoId);
        assertThat(inativos).isEqualTo(1);

        mockMvc.perform(post(PREFIXO + "/veiculos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeVeiculo("ABC1234", clienteId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PLACA_JA_CADASTRADA"));
    }

    private void abrirOrdemServicoComStatus(String veiculoId, String status) {
        jdbc.update("""
                INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, criada_em)
                VALUES (?, ?::uuid, ?::uuid, ?, ?, NOW())
                """, UUID.randomUUID(), clienteId, veiculoId, status, "ACMP-" + UUID.randomUUID());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "EM_EXECUCAO"})
    @DisplayName("deve recusar remocao de veiculo com Ordem de Servico em andamento, com 409")
    void deveRecusarRemocaoComOrdemServicoEmAndamento(String status) throws Exception {
        String veiculoId = cadastrar("ABC1234");
        abrirOrdemServicoComStatus(veiculoId, status);

        mockMvc.perform(delete(PREFIXO + "/veiculos/" + veiculoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VEICULO_COM_ORDEM_SERVICO_EM_ANDAMENTO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FINALIZADA", "ENTREGUE", "CANCELADA"})
    @DisplayName("deve permitir remocao quando a Ordem de Servico ja esta encerrada")
    void devePermitirRemocaoComOrdemServicoEncerrada(String status) throws Exception {
        String veiculoId = cadastrar("ABC1234");
        abrirOrdemServicoComStatus(veiculoId, status);

        mockMvc.perform(delete(PREFIXO + "/veiculos/" + veiculoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
