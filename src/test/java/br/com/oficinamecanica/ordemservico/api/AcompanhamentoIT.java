package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Superficie publica de acompanhamento, sem JWT")
class AcompanhamentoIT extends IntegracaoBase {

    private static final String DOCUMENTO = "10433218100";
    private static final int QUANTIDADE_DE_PECAS = 2;
    private static final int SALDO_INICIAL = 10;

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
    @DisplayName("deve consultar o progresso sem token, com o veiculo e o orcamento discriminado")
    void deveConsultarSemToken() throws Exception {
        String codigo = codigoDeOrdemAguardandoAprovacao();

        mockMvc.perform(get(PREFIXO + "/acompanhamento/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.veiculo").value("Volkswagen Gol ABC1D23"))
                .andExpect(jsonPath("$.orcamentoMaisRecente.versao").value(1))
                .andExpect(jsonPath("$.orcamentoMaisRecente.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.orcamentoMaisRecente.validadeDias").value(10))
                .andExpect(jsonPath("$.orcamentoMaisRecente.total.valor").value(220.00))
                .andExpect(jsonPath("$.orcamentoMaisRecente.itens.length()").value(2))
                .andExpect(jsonPath("$.orcamentoMaisRecente.itens[0].tipo").value("SERVICO"))
                .andExpect(jsonPath("$.orcamentoMaisRecente.itens[0].nome").value("Troca de oleo"))
                .andExpect(jsonPath("$.orcamentoMaisRecente.itens[1].tipo").value("PECA"))
                .andExpect(jsonPath("$.orcamentoMaisRecente.itens[1].nome").value("Filtro de oleo"))
                .andExpect(jsonPath("$.orcamentoMaisRecente.itens[1].quantidade").value(QUANTIDADE_DE_PECAS))
                .andExpect(jsonPath("$.transicoesStatus.length()").value(3));
    }

    @Test
    @DisplayName("nao deve expor o rascunho ainda nao enviado, porque o Cliente so responde ao que recebeu")
    void naoDeveExporORascunhoNaoEnviado() throws Exception {
        String codigo = codigoDeOrdemComRascunho();

        mockMvc.perform(get(PREFIXO + "/acompanhamento/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"))
                .andExpect(jsonPath("$.orcamentoMaisRecente").value(nullValue()));
    }

    @Test
    @DisplayName("deve responder 404 UNIFORME para codigo inexistente e para codigo malformado")
    void deveResponder404Uniforme() throws Exception {
        String inexistente = "ACMP-" + "0".repeat(32);

        String corpoDoInexistente = mockMvc.perform(get(PREFIXO + "/acompanhamento/" + inexistente))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        String corpoDoMalformado = mockMvc.perform(get(PREFIXO + "/acompanhamento/nao-e-um-codigo"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpoDoMalformado).isEqualTo(corpoDoInexistente);
        assertThat(corpoDoInexistente).doesNotContain(inexistente);
    }

    @Test
    @DisplayName("deve aprovar sem token, levar a OS a Em execucao e reservar as pecas da versao aprovada")
    void deveAprovarEReservarAsPecas() throws Exception {
        String codigo = codigoDeOrdemAguardandoAprovacao();

        aprovar(codigo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.orcamentoMaisRecente.situacao").value("APROVADO"))
                .andExpect(jsonPath("$.orcamentoMaisRecente.dataResposta").exists());

        assertThat(contar("SELECT COUNT(*) FROM reserva_peca WHERE peca_id = '" + pecaId + "'"
                + " AND situacao = 'ATIVA'")).isEqualTo(1);
        assertThat(contar("SELECT quantidade_reservada FROM peca WHERE id = '" + pecaId + "'"))
                .isEqualTo(QUANTIDADE_DE_PECAS);
        assertThat(contar("SELECT saldo_em_estoque FROM peca WHERE id = '" + pecaId + "'"))
                .isEqualTo(SALDO_INICIAL);
    }

    @Test
    @DisplayName("deve levar a OS a Cancelada quando o Cliente reprova sem nenhuma versao aprovada")
    void deveCancelarNaReprovacaoSemVersaoAprovada() throws Exception {
        String codigo = codigoDeOrdemAguardandoAprovacao();

        reprovar(codigo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.orcamentoMaisRecente.situacao").value("REPROVADO"));

        assertThat(contar("SELECT COUNT(*) FROM reserva_peca")).isZero();
    }

    @Test
    @DisplayName("deve responder 409 ao aprovar ou reprovar uma OS que ja saiu de Aguardando aprovacao")
    void deveResponder409ForaDeAguardandoAprovacao() throws Exception {
        String codigo = codigoDeOrdemAguardandoAprovacao();
        aprovar(codigo).andExpect(status().isOk());

        aprovar(codigo).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
        reprovar(codigo).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 404 ao aprovar ou reprovar com codigo que nao existe")
    void deveResponder404ComCodigoInexistente() throws Exception {
        String inexistente = "ACMP-" + "f".repeat(32);

        aprovar(inexistente).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ACOMPANHAMENTO_NAO_ENCONTRADO"));
        reprovar(inexistente).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ACOMPANHAMENTO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve responder 409 ao aprovar uma OS que ainda nao recebeu orcamento")
    void deveResponder409SemOrcamentoEnviado() throws Exception {
        String codigo = codigoDeOrdemComRascunho();

        aprovar(codigo).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
    }

    private ResultActions aprovar(String codigo) throws Exception {
        return mockMvc.perform(post(PREFIXO + "/acompanhamento/" + codigo + "/orcamento/aprovacao"));
    }

    private ResultActions reprovar(String codigo) throws Exception {
        return mockMvc.perform(post(PREFIXO + "/acompanhamento/" + codigo + "/orcamento/reprovacao"));
    }

    private String codigoDeOrdemComRascunho() throws Exception {
        String corpo = mockMvc.perform(post(PREFIXO + "/ordens-servico")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentoCliente": "%s", "veiculoId": "%s"}
                                """.formatted(DOCUMENTO, veiculoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
        String codigo = objectMapper.readTree(corpo).get("codigoAcompanhamento").asText();

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/diagnostico/inicio")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/itens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itensServico": [{"servicoId": "%s"}],
                                 "itensPeca": [{"pecaId": "%s", "quantidade": %d}]}
                                """.formatted(servicoId, pecaId, QUANTIDADE_DE_PECAS)))
                .andExpect(status().isOk());
        return codigo;
    }

    private String codigoDeOrdemAguardandoAprovacao() throws Exception {
        String codigo = codigoDeOrdemComRascunho();
        UUID id = jdbc.queryForObject(
                "SELECT id FROM ordem_servico WHERE codigo_acompanhamento = ?", UUID.class, codigo);
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/diagnostico/conclusao")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        return codigo;
    }

    private int contar(String consulta) {
        return jdbc.queryForObject(consulta, Integer.class);
    }
}
