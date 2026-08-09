package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Ciclo completo da OS: reparo adicional, conclusao da execucao e entrega")
class CicloDaOrdemServicoIT extends IntegracaoBase {

    private static final String DOCUMENTO = "10433218100";
    private static final int QUANTIDADE_DE_PECAS = 2;
    private static final int QUANTIDADE_DO_ADICIONAL = 3;
    private static final int SALDO_INICIAL = 20;

    @MockitoBean
    private MailSender mailSender;

    private String token;
    private UUID veiculoId;
    private UUID servicoId;
    private UUID pecaId;

    @BeforeEach
    void prepararCatalogoECadastros() throws Exception {
        token = tokenAdministrativo();
        UUID clienteId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
        servicoId = UUID.randomUUID();
        pecaId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                clienteId, "Ana Beatriz Souza", DOCUMENTO, "ana@example.com");
        jdbc.update("""
                INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id)
                VALUES (?, 'ABC1D23', 'Volkswagen', 'Gol', 2020, ?)
                """, veiculoId, clienteId);
        jdbc.update("""
                INSERT INTO servico (id, nome, descricao, valor_mao_de_obra, moeda)
                VALUES (?, 'Troca de oleo', 'Troca completa', 120.00, 'BRL')
                """, servicoId);
        jdbc.update("""
                INSERT INTO peca (id, nome, unidade_medida, preco, moeda, saldo_em_estoque,
                                  quantidade_reservada, estoque_minimo)
                VALUES (?, 'Filtro de oleo', 'unidade', 50.00, 'BRL', ?, 0, 2)
                """, pecaId, SALDO_INICIAL);
    }

    @Test
    @DisplayName("deve percorrer o ciclo inteiro ate Entregue, reservando as pecas de cada versao aprovada")
    void devePercorrerOCicloInteiro() throws Exception {
        UUID id = ordemEmExecucao();

        reparoAdicional(id, corpoDoReparoAdicional())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.orcamentos.length()").value(2))
                .andExpect(jsonPath("$.orcamentos[1].versao").value(2))
                .andExpect(jsonPath("$.orcamentos[1].descricao").value("Troca da bomba d agua"))
                .andExpect(jsonPath("$.orcamentos[1].dataEnvio").exists());

        aprovar(id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_EXECUCAO"));

        concluirExecucao(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"));
        entregar(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"))
                .andExpect(jsonPath("$.transicoesStatus[*].paraStatus").value(contains(
                        "RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "EM_EXECUCAO",
                        "AGUARDANDO_APROVACAO", "EM_EXECUCAO", "FINALIZADA", "ENTREGUE")));

        assertThat(contar("SELECT COUNT(*) FROM reserva_peca WHERE peca_id = '" + pecaId + "'")).isEqualTo(2);
        assertThat(contar("SELECT quantidade_reservada FROM peca WHERE id = '" + pecaId + "'"))
                .isEqualTo(QUANTIDADE_DE_PECAS + QUANTIDADE_DO_ADICIONAL);
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("deve manter a versao anterior aprovada e imutavel depois do reparo adicional")
    void deveManterAVersaoAnteriorImutavel() throws Exception {
        UUID id = ordemEmExecucao();

        reparoAdicional(id, corpoDoReparoAdicional())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orcamentos[0].versao").value(1))
                .andExpect(jsonPath("$.orcamentos[0].situacao").value("APROVADO"))
                .andExpect(jsonPath("$.orcamentos[0].total.valor").value(220.00))
                .andExpect(jsonPath("$.orcamentos[1].total.valor").value(370.00));
    }

    @Test
    @DisplayName("deve responder 409 ao registrar reparo adicional fora de Em execucao")
    void deveResponder409ComReparoAdicionalForaDeEmExecucao() throws Exception {
        UUID id = ordemAguardandoAprovacao();

        reparoAdicional(id, corpoDoReparoAdicional())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 400 no reparo adicional sem descricao, que o contrato exige")
    void deveResponder400SemDescricao() throws Exception {
        UUID id = ordemEmExecucao();

        reparoAdicional(id, """
                {"itensServico": [{"servicoId": "%s"}]}
                """.formatted(servicoId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 400 quando nenhuma lista de itens vem, porque o contrato exige ao menos uma")
    void deveResponder400SemNenhumaListaDeItens() throws Exception {
        UUID emExecucao = ordemEmExecucao();
        reparoAdicional(emExecucao, """
                {"descricao": "Troca da bomba d agua"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

        UUID emDiagnostico = ordemEmDiagnostico();
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + emDiagnostico + "/itens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 409 ao concluir a execucao ou registrar a entrega fora de ordem")
    void deveResponder409ForaDeOrdem() throws Exception {
        UUID id = ordemAguardandoAprovacao();
        concluirExecucao(id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));

        aprovar(id).andExpect(status().isOk());
        entregar(id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));

        concluirExecucao(id).andExpect(status().isOk());
        entregar(id).andExpect(status().isOk());
        entregar(id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 401 nas rotas administrativas do ciclo sem token")
    void deveResponder401SemToken() throws Exception {
        UUID id = ordemEmExecucao();

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/execucao/conclusao"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/entrega"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/reparos-adicionais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDoReparoAdicional()))
                .andExpect(status().isUnauthorized());
    }

    private String corpoDoReparoAdicional() {
        return """
                {"descricao": "Troca da bomba d agua",
                 "itensPeca": [{"pecaId": "%s", "quantidade": %d}]}
                """.formatted(pecaId, QUANTIDADE_DO_ADICIONAL);
    }

    private ResultActions reparoAdicional(UUID id, String corpo) throws Exception {
        return mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/reparos-adicionais")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions concluirExecucao(UUID id) throws Exception {
        return mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/execucao/conclusao")
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions entregar(UUID id) throws Exception {
        return mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/entrega")
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions aprovar(UUID id) throws Exception {
        String codigo = jdbc.queryForObject(
                "SELECT codigo_acompanhamento FROM ordem_servico WHERE id = ?", String.class, id);
        return mockMvc.perform(post(PREFIXO + "/acompanhamento/" + codigo + "/orcamento/aprovacao"));
    }

    private UUID ordemEmDiagnostico() throws Exception {
        String corpo = mockMvc.perform(post(PREFIXO + "/ordens-servico")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentoCliente": "%s", "veiculoId": "%s"}
                                """.formatted(DOCUMENTO, veiculoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/diagnostico/inicio")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        return id;
    }

    private UUID ordemAguardandoAprovacao() throws Exception {
        UUID id = ordemEmDiagnostico();
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/itens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itensServico": [{"servicoId": "%s"}],
                                 "itensPeca": [{"pecaId": "%s", "quantidade": %d}]}
                                """.formatted(servicoId, pecaId, QUANTIDADE_DE_PECAS)))
                .andExpect(status().isOk());
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/diagnostico/conclusao")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        return id;
    }

    private UUID ordemEmExecucao() throws Exception {
        UUID id = ordemAguardandoAprovacao();
        aprovar(id).andExpect(status().isOk());
        return id;
    }

    private int contar(String consulta) {
        return jdbc.queryForObject(consulta, Integer.class);
    }
}
