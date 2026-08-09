package br.com.oficinamecanica.catalogo.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CRUD de servicos do catalogo")
class ServicoIT extends IntegracaoBase {

    private String token;

    @BeforeEach
    void prepararCenario() throws Exception {
        token = tokenAdministrativo();
    }

    private String corpoDeServico(String nome, String valor) {
        return """
                {"nome": "%s", "descricao": "Inclui filtro", "valorMaoDeObra": {"valor": %s, "moeda": "BRL"}}
                """.formatted(nome, valor);
    }

    private String cadastrar(String nome, String valor) throws Exception {
        String corpo = mockMvc.perform(post(PREFIXO + "/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeServico(nome, valor)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asText();
    }

    private void incluirEmOrdemComStatus(String servicoId, String status) {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID ordemId = UUID.randomUUID();
        UUID orcamentoId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                clienteId, "Ana Beatriz Souza", "10433218100", "ana@example.com");
        jdbc.update("INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id) VALUES (?, ?, ?, ?, ?, ?)",
                veiculoId, "ABC1234", "Fiat", "Uno", 2015, clienteId);
        jdbc.update("""
                INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, criada_em)
                VALUES (?, ?, ?, ?, ?, NOW())
                """, ordemId, clienteId, veiculoId, status, "ACMP-" + ordemId);
        jdbc.update("INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total) VALUES (?, ?, 1, ?, ?)",
                orcamentoId, ordemId, "PENDENTE", new BigDecimal("189.90"));
        jdbc.update("""
                INSERT INTO item_servico (id, ordem_servico_id, orcamento_origem_id, servico_id, valor_mao_de_obra_snapshot)
                VALUES (?, ?, ?, ?::uuid, ?)
                """, UUID.randomUUID(), ordemId, orcamentoId, servicoId, new BigDecimal("189.90"));
    }

    @Test
    @DisplayName("deve cadastrar servico com Valor de mao de obra em BRL")
    void deveCadastrarServico() throws Exception {
        mockMvc.perform(post(PREFIXO + "/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeServico("Troca de oleo", "189.90")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Troca de oleo"))
                .andExpect(jsonPath("$.descricao").value("Inclui filtro"))
                .andExpect(jsonPath("$.valorMaoDeObra.valor").value(189.90))
                .andExpect(jsonPath("$.valorMaoDeObra.moeda").value("BRL"));
    }

    @Test
    @DisplayName("deve cadastrar servico sem descricao, que e opcional")
    void deveCadastrarSemDescricao() throws Exception {
        mockMvc.perform(post(PREFIXO + "/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Alinhamento", "valorMaoDeObra": {"valor": 120, "moeda": "BRL"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorMaoDeObra.valor").value(120.00));
    }

    @Test
    @DisplayName("deve recusar nome em branco, com 400")
    void deveRecusarNomeEmBranco() throws Exception {
        mockMvc.perform(post(PREFIXO + "/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeServico("   ", "189.90")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve recusar Valor de mao de obra negativo, com 400")
    void deveRecusarValorNegativo() throws Exception {
        mockMvc.perform(post(PREFIXO + "/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeServico("Troca de oleo", "-1.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALOR_MONETARIO_INVALIDO"));
    }

    @Test
    @DisplayName("deve recusar Valor de mao de obra com mais de duas casas decimais, com 400")
    void deveRecusarValorComTresCasas() throws Exception {
        mockMvc.perform(post(PREFIXO + "/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeServico("Troca de oleo", "189.999")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALOR_MONETARIO_INVALIDO"));
    }

    @Test
    @DisplayName("deve recusar Valor de mao de obra acima do teto que a coluna persiste, com 400")
    void deveRecusarValorAcimaDoTeto() throws Exception {
        mockMvc.perform(post(PREFIXO + "/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeServico("Troca de oleo", "10000000000.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALOR_MONETARIO_INVALIDO"));
    }

    @Test
    @DisplayName("deve recusar moeda diferente de BRL, com 400")
    void deveRecusarOutraMoeda() throws Exception {
        mockMvc.perform(post(PREFIXO + "/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Troca de oleo", "valorMaoDeObra": {"valor": 189.90, "moeda": "USD"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALOR_MONETARIO_INVALIDO"));
    }

    @Test
    @DisplayName("deve recusar cadastro sem Valor de mao de obra, com 400")
    void deveRecusarSemValor() throws Exception {
        mockMvc.perform(post(PREFIXO + "/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Troca de oleo"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve listar o catalogo em ordem de nome")
    void deveListarEmOrdemDeNome() throws Exception {
        cadastrar("Troca de oleo", "189.90");
        cadastrar("Alinhamento", "120.00");

        mockMvc.perform(get(PREFIXO + "/servicos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Alinhamento"))
                .andExpect(jsonPath("$[1].nome").value("Troca de oleo"));
    }

    @Test
    @DisplayName("deve detalhar o servico do catalogo")
    void deveDetalharServico() throws Exception {
        String servicoId = cadastrar("Troca de oleo", "189.90");

        mockMvc.perform(get(PREFIXO + "/servicos/" + servicoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(servicoId))
                .andExpect(jsonPath("$.valorMaoDeObra.valor").value(189.90));
    }

    @Test
    @DisplayName("deve responder 404 ao detalhar servico inexistente")
    void deveResponder404ParaServicoInexistente() throws Exception {
        mockMvc.perform(get(PREFIXO + "/servicos/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SERVICO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve alterar nome e Valor de mao de obra preservando a identidade")
    void deveAlterarServico() throws Exception {
        String servicoId = cadastrar("Troca de oleo", "189.90");

        mockMvc.perform(put(PREFIXO + "/servicos/" + servicoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeServico("Troca de oleo sintetico", "249.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(servicoId))
                .andExpect(jsonPath("$.nome").value("Troca de oleo sintetico"))
                .andExpect(jsonPath("$.valorMaoDeObra.valor").value(249.00));

        BigDecimal persistido = jdbc.queryForObject(
                "SELECT valor_mao_de_obra FROM servico WHERE id = ?::uuid", BigDecimal.class, servicoId);
        assertThat(persistido).isEqualByComparingTo("249.00");
    }

    @Test
    @DisplayName("deve preservar o snapshot ja copiado para o Item de servico ao alterar o catalogo")
    void devePreservarSnapshotDoItemDeServico() throws Exception {
        String servicoId = cadastrar("Troca de oleo", "189.90");
        incluirEmOrdemComStatus(servicoId, "ENTREGUE");

        mockMvc.perform(put(PREFIXO + "/servicos/" + servicoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeServico("Troca de oleo", "249.00")))
                .andExpect(status().isOk());

        BigDecimal snapshot = jdbc.queryForObject(
                "SELECT valor_mao_de_obra_snapshot FROM item_servico WHERE servico_id = ?::uuid",
                BigDecimal.class, servicoId);
        assertThat(snapshot).isEqualByComparingTo("189.90");
    }

    @Test
    @DisplayName("deve responder 404 ao alterar servico inexistente")
    void deveResponder404AoAlterarInexistente() throws Exception {
        mockMvc.perform(put(PREFIXO + "/servicos/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeServico("Troca de oleo", "189.90")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SERVICO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("deve remover logicamente, sumindo da listagem e do detalhe sem apagar o registro")
    void deveRemoverLogicamente() throws Exception {
        String servicoId = cadastrar("Troca de oleo", "189.90");

        mockMvc.perform(delete(PREFIXO + "/servicos/" + servicoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PREFIXO + "/servicos/" + servicoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SERVICO_NAO_ENCONTRADO"));

        mockMvc.perform(get(PREFIXO + "/servicos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        Integer inativos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM servico WHERE id = ?::uuid AND ativo = FALSE", Integer.class, servicoId);
        assertThat(inativos).isEqualTo(1);
    }

    @Test
    @DisplayName("deve responder 404 ao remover servico inexistente")
    void deveResponder404AoRemoverInexistente() throws Exception {
        mockMvc.perform(delete(PREFIXO + "/servicos/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SERVICO_NAO_ENCONTRADO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "EM_EXECUCAO"})
    @DisplayName("deve recusar remocao de servico em Ordem de Servico em andamento, com 409")
    void deveRecusarRemocaoComOrdemServicoEmAndamento(String status) throws Exception {
        String servicoId = cadastrar("Troca de oleo", "189.90");
        incluirEmOrdemComStatus(servicoId, status);

        mockMvc.perform(delete(PREFIXO + "/servicos/" + servicoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SERVICO_COM_ORDEM_SERVICO_EM_ANDAMENTO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FINALIZADA", "ENTREGUE", "CANCELADA"})
    @DisplayName("deve permitir remocao quando a Ordem de Servico que usa o servico ja esta encerrada")
    void devePermitirRemocaoComOrdemServicoEncerrada(String status) throws Exception {
        String servicoId = cadastrar("Troca de oleo", "189.90");
        incluirEmOrdemComStatus(servicoId, status);

        mockMvc.perform(delete(PREFIXO + "/servicos/" + servicoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("deve responder 400 quando o identificador do caminho nao e UUID")
    void deveResponder400ParaIdentificadorMalformado() throws Exception {
        mockMvc.perform(get(PREFIXO + "/servicos/nao-e-uuid").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "f0000000-0000-4000-8000-1",
            "0f0000000-000-4000-8000-000000000001",
            "f0000000-00000-400-8000-000000000001",
            " "})
    @DisplayName("deve recusar identificador que nao esteja no formato canonico, sem resolver outro servico")
    void deveRecusarIdentificadorForaDoFormato(String identificador) throws Exception {
        mockMvc.perform(get(PREFIXO + "/servicos/{id}", identificador)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve aceitar identificador em caixa alta e resolver o servico certo")
    void deveAceitarIdentificadorEmCaixaAlta() throws Exception {
        String servicoId = cadastrar("Troca de oleo", "189.90");

        mockMvc.perform(get(PREFIXO + "/servicos/" + servicoId.toUpperCase(Locale.ROOT))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(servicoId));
    }

    @Test
    @DisplayName("deve recusar alteracao e remocao por identificador fora do formato, sem tocar no catalogo")
    void deveRecusarEscritaPorIdentificadorForaDoFormato() throws Exception {
        String servicoId = cadastrar("Troca de oleo", "189.90");
        String disfarce = "0f0000000-000-4000-8000-000000000001";

        mockMvc.perform(put(PREFIXO + "/servicos/" + disfarce)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeServico("Invadido", "1.00")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete(PREFIXO + "/servicos/" + disfarce).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(PREFIXO + "/servicos/" + servicoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Troca de oleo"));
    }
}
