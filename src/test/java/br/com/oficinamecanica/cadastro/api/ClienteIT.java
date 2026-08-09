package br.com.oficinamecanica.cadastro.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CRUD de clientes")
class ClienteIT extends IntegracaoBase {

    private static final String CPF = "10433218100";
    private static final String CNPJ = "31034131000113";

    private String token;

    @BeforeEach
    void autenticarComoAdministrador() throws Exception {
        token = tokenAdministrativo();
    }

    private String corpoDeCliente(String nome, String documento, String email) {
        return """
                {"nome": "%s", "documento": "%s", "email": "%s", "telefone": "11999990000"}
                """.formatted(nome, documento, email);
    }

    private String cadastrar(String nome, String documento, String email) throws Exception {
        String corpo = mockMvc.perform(post(PREFIXO + "/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCliente(nome, documento, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asText();
    }

    @Test
    @DisplayName("deve cadastrar cliente pessoa fisica e devolver a identidade gerada")
    void deveCadastrarPessoaFisica() throws Exception {
        mockMvc.perform(post(PREFIXO + "/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCliente("Ana Beatriz Souza", CPF, "ana@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.documento").value(CPF))
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    @DisplayName("deve cadastrar cliente pessoa juridica no mesmo agregado")
    void deveCadastrarPessoaJuridica() throws Exception {
        mockMvc.perform(post(PREFIXO + "/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCliente("Transportadora Rota Azul", CNPJ, "frota@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documento").value(CNPJ));
    }

    @Test
    @DisplayName("deve recusar Documento com digito verificador invalido, com 400")
    void deveRecusarDocumentoInvalido() throws Exception {
        mockMvc.perform(post(PREFIXO + "/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCliente("Ana", "10433218199", "ana@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DOCUMENTO_INVALIDO"));
    }

    @Test
    @DisplayName("deve recusar e-mail ausente, porque e o destino do envio do Orcamento")
    void deveRecusarSemEmail() throws Exception {
        mockMvc.perform(post(PREFIXO + "/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana", "documento": "%s"}
                                """.formatted(CPF)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve recusar Documento ja cadastrado, com 409")
    void deveRecusarDocumentoDuplicado() throws Exception {
        cadastrar("Ana", CPF, "ana@example.com");

        mockMvc.perform(post(PREFIXO + "/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCliente("Outra pessoa", CPF, "outra@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DOCUMENTO_JA_CADASTRADO"));
    }

    @Test
    @DisplayName("deve devolver 409 e nunca 500 quando duas requisicoes simultaneas cadastram o mesmo Documento")
    void deveDevolver409NaCorridaPeloMesmoDocumento() throws Exception {
        int simultaneas = 12;
        ExecutorService executor = Executors.newFixedThreadPool(simultaneas);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> respostas = new ArrayList<>();
        for (int i = 0; i < simultaneas; i++) {
            respostas.add(executor.submit(() -> {
                largada.await();
                return mockMvc.perform(post(PREFIXO + "/clientes")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoDeCliente("Corrida", CPF, "corrida@example.com")))
                        .andReturn().getResponse().getStatus();
            }));
        }
        largada.countDown();

        List<Integer> status = new ArrayList<>();
        for (Future<Integer> resposta : respostas) {
            status.add(resposta.get(30, TimeUnit.SECONDS));
        }
        executor.shutdown();

        assertThat(status).doesNotContain(500);
        assertThat(status).containsOnly(201, 409);
        assertThat(status.stream().filter(codigo -> codigo == 201).count()).isEqualTo(1);
        Integer gravados = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cliente WHERE documento = ?", Integer.class, CPF);
        assertThat(gravados).isEqualTo(1);
    }

    @Test
    @DisplayName("deve detalhar cliente cadastrado")
    void deveDetalhar() throws Exception {
        String id = cadastrar("Ana", CPF, "ana@example.com");

        mockMvc.perform(get(PREFIXO + "/clientes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana"));
    }

    @Test
    @DisplayName("deve responder 404 para cliente inexistente")
    void deveResponder404ParaInexistente() throws Exception {
        mockMvc.perform(get(PREFIXO + "/clientes/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve responder 400 com o corpo do contrato quando o id do caminho nao e um UUID")
    void deveResponder400ParaIdMalformado() throws Exception {
        mockMvc.perform(get(PREFIXO + "/clientes/nao-e-uuid").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 400 quando o filtro por Documento tem formato invalido")
    void deveResponder400ParaFiltroInvalido() throws Exception {
        mockMvc.perform(get(PREFIXO + "/clientes").param("documento", "123")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DOCUMENTO_INVALIDO"));
    }

    @Test
    @DisplayName("deve listar clientes ativos e filtrar por Documento")
    void deveListarEFiltrar() throws Exception {
        cadastrar("Ana", CPF, "ana@example.com");
        cadastrar("Transportadora", CNPJ, "frota@example.com");

        mockMvc.perform(get(PREFIXO + "/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get(PREFIXO + "/clientes").param("documento", CPF)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].documento").value(CPF));
    }

    @Test
    @DisplayName("deve alterar cliente preservando a identidade")
    void deveAlterar() throws Exception {
        String id = cadastrar("Ana", CPF, "ana@example.com");

        mockMvc.perform(put(PREFIXO + "/clientes/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCliente("Ana Beatriz Souza", CPF, "nova@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nome").value("Ana Beatriz Souza"))
                .andExpect(jsonPath("$.email").value("nova@example.com"));
    }

    @Test
    @DisplayName("deve remover logicamente: some da listagem e do detalhe, e o registro fica no banco")
    void deveRemoverLogicamente() throws Exception {
        String id = cadastrar("Ana", CPF, "ana@example.com");

        mockMvc.perform(delete(PREFIXO + "/clientes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PREFIXO + "/clientes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(PREFIXO + "/clientes").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));

        Integer linhas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cliente WHERE id = ?::uuid AND ativo = FALSE", Integer.class, id);
        assertThat(linhas).isEqualTo(1);
    }

    @Test
    @DisplayName("deve manter o Documento reservado depois da remocao logica, recusando recadastro com 409")
    void deveManterDocumentoReservadoAposRemocao() throws Exception {
        String id = cadastrar("Ana", CPF, "ana@example.com");
        mockMvc.perform(delete(PREFIXO + "/clientes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(PREFIXO + "/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCliente("Ana de novo", CPF, "ana2@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DOCUMENTO_JA_CADASTRADO"));
    }

    private void abrirOrdemServicoComStatus(String clienteId, String status) {
        UUID veiculoId = UUID.randomUUID();
        jdbc.update("INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id) VALUES (?, ?, ?, ?, ?, ?::uuid)",
                veiculoId, "ABC1234", "Fiat", "Uno", 2015, clienteId);
        jdbc.update("""
                INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, criada_em)
                VALUES (?, ?::uuid, ?, ?, ?, NOW())
                """, UUID.randomUUID(), clienteId, veiculoId, status, "ACMP-" + UUID.randomUUID());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "EM_EXECUCAO"})
    @DisplayName("deve recusar remocao de cliente com Ordem de Servico em andamento, com 409")
    void deveRecusarRemocaoComOrdemServicoEmAndamento(String status) throws Exception {
        String clienteId = cadastrar("Ana", CPF, "ana@example.com");
        abrirOrdemServicoComStatus(clienteId, status);

        mockMvc.perform(delete(PREFIXO + "/clientes/" + clienteId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_COM_ORDEM_SERVICO_EM_ANDAMENTO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FINALIZADA", "ENTREGUE", "CANCELADA"})
    @DisplayName("deve permitir remocao quando a Ordem de Servico do cliente ja esta encerrada")
    void devePermitirRemocaoComOrdemServicoEncerrada(String status) throws Exception {
        String clienteId = cadastrar("Ana", CPF, "ana@example.com");
        abrirOrdemServicoComStatus(clienteId, status);

        mockMvc.perform(delete(PREFIXO + "/clientes/" + clienteId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
