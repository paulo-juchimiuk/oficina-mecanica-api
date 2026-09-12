package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.ResultActions;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Orcamento versionado: inclusao de itens e conclusao do diagnostico")
class OrcamentoIT extends IntegracaoBase {

    private static final String DOCUMENTO = "10433218100";
    private static final String ASSUNTO_DO_ORCAMENTO = "Orcamento da sua Ordem de Servico";
    private static final String EMAIL_DO_CLIENTE = "ana@example.com";

    private String token;
    private UUID clienteId;
    private UUID veiculoId;
    private UUID servicoId;
    private UUID pecaId;

    @BeforeEach
    void prepararCatalogoECadastros() throws Exception {
        token = tokenAdministrativo();
        clienteId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
        servicoId = UUID.randomUUID();
        pecaId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                clienteId, "Ana Beatriz Souza", DOCUMENTO, EMAIL_DO_CLIENTE);
        jdbc.update("""
                INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id)
                VALUES (?, 'ABC1234', 'Fiat', 'Uno', 2015, ?)
                """, veiculoId, clienteId);
        jdbc.update("""
                INSERT INTO servico (id, nome, descricao, valor_mao_de_obra, moeda)
                VALUES (?, 'Troca de oleo', 'Troca completa', 120.00, 'BRL')
                """, servicoId);
        jdbc.update("""
                INSERT INTO peca (id, nome, unidade_medida, preco, moeda, saldo_em_estoque,
                                  quantidade_reservada, estoque_minimo)
                VALUES (?, 'Filtro de oleo', 'unidade', 50.00, 'BRL', 10, 0, 2)
                """, pecaId);
    }

    @Test
    @DisplayName("deve abrir a versao 1 como rascunho pendente, somando servicos e pecas no total")
    void deveAbrirAVersaoUmComoRascunho() throws Exception {
        UUID id = ordemEmDiagnostico();

        incluirItens(id, corpoComServicoEPeca(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orcamentos.length()").value(1))
                .andExpect(jsonPath("$.orcamentos[0].versao").value(1))
                .andExpect(jsonPath("$.orcamentos[0].situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.orcamentos[0].validadeDias").value(10))
                .andExpect(jsonPath("$.orcamentos[0].dataEnvio").value(nullValue()))
                .andExpect(jsonPath("$.orcamentos[0].total.valor").value(220.00))
                .andExpect(jsonPath("$.orcamentos[0].total.moeda").value("BRL"))
                .andExpect(jsonPath("$.itensServico.length()").value(1))
                .andExpect(jsonPath("$.itensServico[0].valorMaoDeObraSnapshot.valor").value(120.00))
                .andExpect(jsonPath("$.itensServico[0].versaoOrcamento").value(1))
                .andExpect(jsonPath("$.itensPeca[0].quantidade").value(2))
                .andExpect(jsonPath("$.itensPeca[0].precoSnapshot.valor").value(50.00));
    }

    @Test
    @DisplayName("deve vincular cada item a versao que o introduziu, na coluna de origem")
    void deveVincularOItemAVersaoDeOrigem() throws Exception {
        UUID id = ordemEmDiagnostico();
        incluirItens(id, corpoComServicoEPeca(1)).andExpect(status().isOk());

        UUID orcamentoId = jdbc.queryForObject(
                "SELECT id FROM orcamento WHERE ordem_servico_id = ? AND versao = 1", UUID.class, id);
        assertThat(jdbc.queryForObject(
                "SELECT orcamento_origem_id FROM item_servico WHERE ordem_servico_id = ?", UUID.class, id))
                .isEqualTo(orcamentoId);
        assertThat(jdbc.queryForObject(
                "SELECT orcamento_origem_id FROM item_peca WHERE ordem_servico_id = ?", UUID.class, id))
                .isEqualTo(orcamentoId);
    }

    @Test
    @DisplayName("deve acumular na mesma versao sem duplicar orcamento nem itens ja gravados")
    void deveAcumularSemDuplicar() throws Exception {
        UUID id = ordemEmDiagnostico();

        incluirItens(id, corpoComServicoEPeca(1)).andExpect(status().isOk());
        incluirItens(id, corpoComServico()).andExpect(status().isOk())
                .andExpect(jsonPath("$.orcamentos.length()").value(1))
                .andExpect(jsonPath("$.orcamentos[0].total.valor").value(290.00));

        assertThat(contar("SELECT COUNT(*) FROM orcamento WHERE ordem_servico_id = '" + id + "'")).isEqualTo(1);
        assertThat(contar("SELECT COUNT(*) FROM item_servico WHERE ordem_servico_id = '" + id + "'")).isEqualTo(2);
        assertThat(contar("SELECT COUNT(*) FROM item_peca WHERE ordem_servico_id = '" + id + "'")).isEqualTo(1);
    }

    @Test
    @DisplayName("deve responder 404 quando o Servico do item nao esta ativo no catalogo")
    void deveResponder404ComServicoInexistente() throws Exception {
        UUID id = ordemEmDiagnostico();

        incluirItens(id, """
                {"itensServico": [{"servicoId": "%s"}]}
                """.formatted(UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SERVICO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve responder 404 quando a Peca do item nao esta ativa no catalogo")
    void deveResponder404ComPecaInexistente() throws Exception {
        UUID id = ordemEmDiagnostico();

        incluirItens(id, """
                {"itensPeca": [{"pecaId": "%s", "quantidade": 1}]}
                """.formatted(UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PECA_NAO_ENCONTRADA"));
    }

    @Test
    @DisplayName("deve aceitar itens na OS Recebida, abrindo a versao 1 antes do diagnostico")
    void deveAceitarItensNaOrdemRecebida() throws Exception {
        UUID id = criarOrdem();

        incluirItens(id, corpoComServico())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.orcamentos.length()").value(1))
                .andExpect(jsonPath("$.orcamentos[0].versao").value(1));
    }

    @Test
    @DisplayName("deve responder 409 ao incluir itens depois que o diagnostico foi concluido")
    void deveResponder409AoIncluirItensDepoisDoDiagnostico() throws Exception {
        UUID id = ordemEmDiagnostico();
        incluirItens(id, corpoComServico()).andExpect(status().isOk());
        concluirDiagnostico(id).andExpect(status().isOk());

        incluirItens(id, corpoComServico())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve enviar a versao ao Cliente e levar a OS a Aguardando aprovacao")
    void deveEnviarOOrcamentoEMudarOStatus() throws Exception {
        UUID id = ordemEmDiagnostico();
        incluirItens(id, corpoComServicoEPeca(2)).andExpect(status().isOk());

        concluirDiagnostico(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.orcamentos[0].dataEnvio").exists())
                .andExpect(jsonPath("$.orcamentos[0].situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.transicoesStatus.length()").value(3));

        assertThat(emailsComAssunto(ASSUNTO_DO_ORCAMENTO)).hasSize(1);
    }

    @Test
    @DisplayName("deve enviar ao Cliente um e-mail com destinatario, assunto e o link que carrega o Codigo")
    void deveEnviarEmailComOLinkDoCodigo() throws Exception {
        UUID id = ordemEmDiagnostico();
        incluirItens(id, corpoComServicoEPeca(2)).andExpect(status().isOk());
        String codigo = jdbc.queryForObject(
                "SELECT codigo_acompanhamento FROM ordem_servico WHERE id = ?", String.class, id);

        concluirDiagnostico(id).andExpect(status().isOk());

        assertThat(emailsComAssunto(ASSUNTO_DO_ORCAMENTO)).hasSize(1);
        SimpleMailMessage mensagem = emailsComAssunto(ASSUNTO_DO_ORCAMENTO).getFirst();
        assertThat(mensagem.getTo()).containsExactly(EMAIL_DO_CLIENTE);
        assertThat(mensagem.getText())
                .contains(codigo)
                .contains("/acompanhamento/" + codigo)
                .contains("220.00")
                .contains("AGUARDANDO_APROVACAO");
    }

    @Test
    @DisplayName("deve responder 409 ao concluir o diagnostico sem nenhum item, porque nao ha Orcamento a enviar")
    void deveResponder409AoConcluirSemItens() throws Exception {
        UUID id = ordemEmDiagnostico();

        concluirDiagnostico(id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ORDEM_SERVICO_SEM_ORCAMENTO"));
    }

    @Test
    @DisplayName("deve responder 409 ao incluir itens depois de a versao ter sido enviada")
    void deveResponder409AoIncluirItensDepoisDoEnvio() throws Exception {
        UUID id = ordemEmDiagnostico();
        incluirItens(id, corpoComServico()).andExpect(status().isOk());
        concluirDiagnostico(id).andExpect(status().isOk());

        incluirItens(id, corpoComServico())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 400 quando a requisicao de itens vem sem quantidade da peca")
    void deveResponder400SemQuantidade() throws Exception {
        UUID id = ordemEmDiagnostico();

        incluirItens(id, """
                {"itensPeca": [{"pecaId": "%s"}]}
                """.formatted(pecaId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 401 nas rotas de orcamento sem token administrativo")
    void deveResponder401SemToken() throws Exception {
        UUID id = ordemEmDiagnostico();

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/itens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComServico()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/diagnostico/conclusao"))
                .andExpect(status().isUnauthorized());
    }

    private String corpoComServico() {
        return """
                {"itensServico": [{"servicoId": "%s"}]}
                """.formatted(servicoId);
    }

    private String corpoComServicoEPeca(int quantidade) {
        return """
                {"itensServico": [{"servicoId": "%s"}],
                 "itensPeca": [{"pecaId": "%s", "quantidade": %d}]}
                """.formatted(servicoId, pecaId, quantidade);
    }

    private ResultActions incluirItens(UUID id, String corpo) throws Exception {
        return mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/itens")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions concluirDiagnostico(UUID id) throws Exception {
        return mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/diagnostico/conclusao")
                .header("Authorization", "Bearer " + token));
    }

    private UUID criarOrdem() throws Exception {
        String corpo = mockMvc.perform(post(PREFIXO + "/ordens-servico")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentoCliente": "%s", "veiculoId": "%s"}
                                """.formatted(DOCUMENTO, veiculoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(corpo).get("id").asText());
    }

    private UUID ordemEmDiagnostico() throws Exception {
        UUID id = criarOrdem();
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + id + "/diagnostico/inicio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return id;
    }

    private int contar(String consulta) {
        return jdbc.queryForObject(consulta, Integer.class);
    }
}
