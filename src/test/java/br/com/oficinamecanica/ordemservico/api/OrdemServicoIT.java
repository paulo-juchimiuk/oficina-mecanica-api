package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Ciclo da Ordem de Servico: abertura, fila e inicio do diagnostico")
class OrdemServicoIT extends IntegracaoBase {

    private static final String DOCUMENTO = "10433218100";
    private static final String DOCUMENTO_DE_OUTRO = "96001338914";
    private static final String RELATO = "Barulho ao frear em baixa velocidade";

    private String token;
    private UUID clienteId;
    private UUID veiculoId;

    @BeforeEach
    void prepararCadastros() throws Exception {
        token = tokenAdministrativo();
        clienteId = inserirCliente(DOCUMENTO, "Ana Beatriz Souza", true);
        veiculoId = inserirVeiculo("ABC1234", clienteId, true);
    }

    @Test
    @DisplayName("deve abrir a OS Recebida, com Codigo de acompanhamento e a transicao de abertura")
    void deveAbrirAOrdemServico() throws Exception {
        criar(DOCUMENTO, veiculoId, RELATO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.clienteId").value(clienteId.toString()))
                .andExpect(jsonPath("$.veiculoId").value(veiculoId.toString()))
                .andExpect(jsonPath("$.relatoDoProblema").value(RELATO))
                .andExpect(jsonPath("$.codigoAcompanhamento").value(matchesPattern("^ACMP-[0-9a-f]{32}$")))
                .andExpect(jsonPath("$.transicoesStatus.length()").value(1))
                .andExpect(jsonPath("$.transicoesStatus[0].deStatus").doesNotExist())
                .andExpect(jsonPath("$.transicoesStatus[0].paraStatus").value("RECEBIDA"));
    }

    @Test
    @DisplayName("deve gerar Codigo de acompanhamento distinto para cada OS")
    void deveGerarCodigoDistintoPorOrdem() throws Exception {
        String primeiro = codigoDaOrdemCriada();
        String segundo = codigoDaOrdemCriada();
        assertThat(primeiro).isNotEqualTo(segundo);
    }

    @Test
    @DisplayName("deve responder 404 quando nenhum Cliente ativo tem o Documento informado")
    void deveResponder404SemCliente() throws Exception {
        criar("64752553000183", veiculoId, RELATO)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve responder 404 quando o Cliente do Documento esta inativo")
    void deveResponder404ComClienteInativo() throws Exception {
        UUID inativo = inserirCliente("02654235114", "Cliente Inativo", false);
        UUID veiculoDoInativo = inserirVeiculo("XYZ9A87", inativo, true);

        criar("02654235114", veiculoDoInativo, RELATO)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve responder 404 quando o Veiculo nao existe")
    void deveResponder404SemVeiculo() throws Exception {
        criar(DOCUMENTO, UUID.randomUUID(), RELATO)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("VEICULO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve responder 409 quando o Veiculo pertence a outro Cliente")
    void deveResponder409ComVeiculoDeOutroCliente() throws Exception {
        UUID outroCliente = inserirCliente(DOCUMENTO_DE_OUTRO, "Carlos Menezes", true);
        UUID veiculoDoOutro = inserirVeiculo("QWE4R56", outroCliente, true);

        criar(DOCUMENTO, veiculoDoOutro, RELATO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VEICULO_DE_OUTRO_CLIENTE"));
    }

    @Test
    @DisplayName("deve responder 400 quando o Documento nao respeita o padrao do contrato")
    void deveResponder400ComDocumentoForaDoPadrao() throws Exception {
        criar("123", veiculoId, RELATO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 401 na abertura sem token administrativo")
    void deveResponder401SemToken() throws Exception {
        mockMvc.perform(post(PREFIXO + "/ordens-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDaCriacao(DOCUMENTO, veiculoId, RELATO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("deve listar a fila de OSs e filtrar por Status da OS")
    void deveListarEFiltrarPorStatus() throws Exception {
        UUID emDiagnostico = idDaOrdemCriada();
        idDaOrdemCriada();
        iniciarDiagnostico(emDiagnostico).andExpect(status().isOk());

        mockMvc.perform(get(PREFIXO + "/ordens-servico").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get(PREFIXO + "/ordens-servico?status=EM_DIAGNOSTICO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(emDiagnostico.toString()));
    }

    @Test
    @DisplayName("deve detalhar a OS e responder 404 para identificador inexistente")
    void deveDetalharARespostaERecusarInexistente() throws Exception {
        UUID id = idDaOrdemCriada();

        mockMvc.perform(get(PREFIXO + "/ordens-servico/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        mockMvc.perform(get(PREFIXO + "/ordens-servico/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ORDEM_SERVICO_NAO_ENCONTRADA"));
    }

    @Test
    @DisplayName("deve levar a OS a Em diagnostico e registrar a segunda transicao, sem duplicar a primeira")
    void deveIniciarDiagnosticoSemDuplicarTransicoes() throws Exception {
        UUID id = idDaOrdemCriada();

        iniciarDiagnostico(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"))
                .andExpect(jsonPath("$.transicoesStatus.length()").value(2))
                .andExpect(jsonPath("$.transicoesStatus[1].deStatus").value("RECEBIDA"))
                .andExpect(jsonPath("$.transicoesStatus[1].paraStatus").value("EM_DIAGNOSTICO"));

        assertThat(contar("SELECT COUNT(*) FROM transicao_status WHERE ordem_servico_id = '" + id + "'"))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("deve recusar com 409 iniciar o diagnostico de uma OS que ja saiu de Recebida")
    void deveRecusarSegundoInicioDeDiagnostico() throws Exception {
        UUID id = idDaOrdemCriada();
        iniciarDiagnostico(id).andExpect(status().isOk());

        iniciarDiagnostico(id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));

        assertThat(contar("SELECT COUNT(*) FROM transicao_status WHERE ordem_servico_id = '" + id + "'"))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("deve responder 404 ao iniciar o diagnostico de uma OS inexistente")
    void deveResponder404AoIniciarDiagnosticoDeOrdemInexistente() throws Exception {
        iniciarDiagnostico(UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ORDEM_SERVICO_NAO_ENCONTRADA"));
    }

    private ResultActions criar(
            String documento, UUID veiculo, String relato) throws Exception {
        return mockMvc.perform(post(PREFIXO + "/ordens-servico")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoDaCriacao(documento, veiculo, relato)));
    }

    private ResultActions iniciarDiagnostico(UUID id) throws Exception {
        return mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/diagnostico/inicio")
                .header("Authorization", "Bearer " + token));
    }

    private String corpoDaCriacao(String documento, UUID veiculo, String relato) {
        return """
                {"documentoCliente": "%s", "veiculoId": "%s", "relatoDoProblema": "%s"}
                """.formatted(documento, veiculo, relato);
    }

    private UUID idDaOrdemCriada() throws Exception {
        return UUID.fromString(campoDaOrdemCriada("id"));
    }

    private String codigoDaOrdemCriada() throws Exception {
        return campoDaOrdemCriada("codigoAcompanhamento");
    }

    private String campoDaOrdemCriada(String campo) throws Exception {
        String corpo = criar(DOCUMENTO, veiculoId, RELATO)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get(campo).asText();
    }

    private UUID inserirCliente(String documento, String nome, boolean ativo) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email, ativo) VALUES (?, ?, ?, ?, ?)",
                id, nome, documento, "cliente" + documento + "@example.com", ativo);
        return id;
    }

    private UUID inserirVeiculo(String placa, UUID cliente, boolean ativo) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id, ativo)
                VALUES (?, ?, 'Fiat', 'Uno', 2015, ?, ?)
                """, id, placa, cliente, ativo);
        return id;
    }

    private int contar(String consulta) {
        return jdbc.queryForObject(consulta, Integer.class);
    }
}
