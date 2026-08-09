package br.com.oficinamecanica.estoque.api;

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
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CRUD de pecas e Entrada de estoque")
class PecaIT extends IntegracaoBase {

    private final AtomicInteger sequencia = new AtomicInteger();

    private String token;

    @BeforeEach
    void prepararCenario() throws Exception {
        token = tokenAdministrativo();
    }

    private String corpoDePeca(String nome, String preco, int estoqueMinimo) {
        return """
                {"nome": "%s", "unidadeMedida": "unidade", "preco": {"valor": %s, "moeda": "BRL"}, "estoqueMinimo": %d}
                """.formatted(nome, preco, estoqueMinimo);
    }

    private String cadastrar(String nome, String preco, int estoqueMinimo) throws Exception {
        String corpo = mockMvc.perform(post(PREFIXO + "/pecas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDePeca(nome, preco, estoqueMinimo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asText();
    }

    private void registrarEntrada(String pecaId, int quantidade) throws Exception {
        mockMvc.perform(post(PREFIXO + "/pecas/" + pecaId + "/entradas-estoque")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantidade": %d}
                                """.formatted(quantidade)))
                .andExpect(status().isOk());
    }

    private UUID criarOrdemComStatus(String status) {
        int ordinal = sequencia.incrementAndGet();
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID ordemId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                clienteId, "Ana Beatriz Souza", "%011d".formatted(ordinal), "ana@example.com");
        jdbc.update("INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id) VALUES (?, ?, ?, ?, ?, ?)",
                veiculoId, "ABC%04d".formatted(ordinal), "Fiat", "Uno", 2015, clienteId);
        jdbc.update("""
                INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, criada_em)
                VALUES (?, ?, ?, ?, ?, NOW())
                """, ordemId, clienteId, veiculoId, status, "ACMP-" + ordemId);
        return ordemId;
    }

    private void incluirEmOrdemComStatus(String pecaId, String status) {
        UUID ordemId = criarOrdemComStatus(status);
        UUID orcamentoId = UUID.randomUUID();
        jdbc.update("INSERT INTO orcamento (id, ordem_servico_id, versao, situacao, total) VALUES (?, ?, 1, ?, ?)",
                orcamentoId, ordemId, "PENDENTE", new BigDecimal("189.90"));
        jdbc.update("""
                INSERT INTO item_peca (id, ordem_servico_id, orcamento_origem_id, peca_id, quantidade, preco_snapshot)
                VALUES (?, ?, ?, ?::uuid, 1, ?)
                """, UUID.randomUUID(), ordemId, orcamentoId, pecaId, new BigDecimal("189.90"));
    }

    private void reservarNoBanco(String pecaId, String situacao) {
        UUID ordemId = criarOrdemComStatus("ENTREGUE");
        jdbc.update("""
                INSERT INTO reserva_peca (id, ordem_servico_id, peca_id, quantidade, situacao, criada_em)
                VALUES (?, ?, ?::uuid, 1, ?, NOW())
                """, UUID.randomUUID(), ordemId, pecaId, situacao);
    }

    @Test
    @DisplayName("deve cadastrar peca com Saldo em estoque zerado")
    void deveCadastrarComSaldoZerado() throws Exception {
        mockMvc.perform(post(PREFIXO + "/pecas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDePeca("Filtro de oleo", "35.90", 10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Filtro de oleo"))
                .andExpect(jsonPath("$.unidadeMedida").value("unidade"))
                .andExpect(jsonPath("$.preco.valor").value(35.90))
                .andExpect(jsonPath("$.preco.moeda").value("BRL"))
                .andExpect(jsonPath("$.estoqueMinimo").value(10))
                .andExpect(jsonPath("$.saldoEmEstoque").value(0))
                .andExpect(jsonPath("$.quantidadeReservada").value(0));
    }

    @Test
    @DisplayName("deve aplicar a unidade de medida padrao quando ela nao vem no corpo")
    void deveAplicarUnidadePadrao() throws Exception {
        mockMvc.perform(post(PREFIXO + "/pecas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Filtro de oleo", "preco": {"valor": 35.90, "moeda": "BRL"}, "estoqueMinimo": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unidadeMedida").value("unidade"));
    }

    @Test
    @DisplayName("deve cadastrar Insumo com unidade de medida propria")
    void deveCadastrarInsumo() throws Exception {
        mockMvc.perform(post(PREFIXO + "/pecas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Oleo lubrificante 5W30", "unidadeMedida": "litro",
                                 "preco": {"valor": 52.00, "moeda": "BRL"}, "estoqueMinimo": 20}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unidadeMedida").value("litro"));
    }

    @Test
    @DisplayName("deve recusar nome em branco, com 400")
    void deveRecusarNomeEmBranco() throws Exception {
        mockMvc.perform(post(PREFIXO + "/pecas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDePeca("   ", "35.90", 10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve recusar preco negativo, com 400")
    void deveRecusarPrecoNegativo() throws Exception {
        mockMvc.perform(post(PREFIXO + "/pecas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDePeca("Filtro de oleo", "-1.00", 10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALOR_MONETARIO_INVALIDO"));
    }

    @Test
    @DisplayName("deve recusar Estoque minimo negativo, com 400")
    void deveRecusarEstoqueMinimoNegativo() throws Exception {
        mockMvc.perform(post(PREFIXO + "/pecas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDePeca("Filtro de oleo", "35.90", -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve recusar cadastro sem Estoque minimo, com 400")
    void deveRecusarSemEstoqueMinimo() throws Exception {
        mockMvc.perform(post(PREFIXO + "/pecas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Filtro de oleo", "preco": {"valor": 35.90, "moeda": "BRL"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve listar as pecas em ordem de nome")
    void deveListarEmOrdemDeNome() throws Exception {
        cadastrar("Filtro de oleo", "35.90", 10);
        cadastrar("Correia dentada", "240.00", 3);

        mockMvc.perform(get(PREFIXO + "/pecas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Correia dentada"))
                .andExpect(jsonPath("$[1].nome").value("Filtro de oleo"));
    }

    @Test
    @DisplayName("deve entregar apenas as pecas abaixo do Estoque minimo quando o filtro e pedido")
    void deveFiltrarAbaixoDoEstoqueMinimo() throws Exception {
        String abastecida = cadastrar("Filtro de oleo", "35.90", 10);
        cadastrar("Correia dentada", "240.00", 3);
        registrarEntrada(abastecida, 24);

        mockMvc.perform(get(PREFIXO + "/pecas?abaixoDoMinimo=true").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Correia dentada"));
    }

    @Test
    @DisplayName("deve detalhar a peca com saldo e reserva")
    void deveDetalharPeca() throws Exception {
        String pecaId = cadastrar("Filtro de oleo", "35.90", 10);

        mockMvc.perform(get(PREFIXO + "/pecas/" + pecaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pecaId))
                .andExpect(jsonPath("$.saldoEmEstoque").value(0));
    }

    @Test
    @DisplayName("deve responder 404 ao detalhar peca inexistente")
    void deveResponder404ParaPecaInexistente() throws Exception {
        mockMvc.perform(get(PREFIXO + "/pecas/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PECA_NAO_ENCONTRADA"));
    }

    @Test
    @DisplayName("deve alterar preco e Estoque minimo preservando o Saldo em estoque")
    void deveAlterarPreservandoSaldo() throws Exception {
        String pecaId = cadastrar("Filtro de oleo", "35.90", 10);
        registrarEntrada(pecaId, 24);

        mockMvc.perform(put(PREFIXO + "/pecas/" + pecaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDePeca("Filtro de oleo premium", "42.00", 12)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pecaId))
                .andExpect(jsonPath("$.nome").value("Filtro de oleo premium"))
                .andExpect(jsonPath("$.preco.valor").value(42.00))
                .andExpect(jsonPath("$.estoqueMinimo").value(12))
                .andExpect(jsonPath("$.saldoEmEstoque").value(24));

        Integer saldo = jdbc.queryForObject(
                "SELECT saldo_em_estoque FROM peca WHERE id = ?::uuid", Integer.class, pecaId);
        assertThat(saldo).isEqualTo(24);
    }

    @Test
    @DisplayName("deve responder 404 ao alterar peca inexistente")
    void deveResponder404AoAlterarInexistente() throws Exception {
        mockMvc.perform(put(PREFIXO + "/pecas/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDePeca("Filtro de oleo", "35.90", 10)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PECA_NAO_ENCONTRADA"));
    }

    @Test
    @DisplayName("deve somar a Entrada de estoque ao saldo e persistir")
    void deveRegistrarEntradaDeEstoque() throws Exception {
        String pecaId = cadastrar("Filtro de oleo", "35.90", 10);

        mockMvc.perform(post(PREFIXO + "/pecas/" + pecaId + "/entradas-estoque")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantidade": 24}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoEmEstoque").value(24));

        registrarEntrada(pecaId, 6);

        Integer saldo = jdbc.queryForObject(
                "SELECT saldo_em_estoque FROM peca WHERE id = ?::uuid", Integer.class, pecaId);
        assertThat(saldo).isEqualTo(30);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("deve recusar Entrada de estoque sem quantidade positiva, com 400")
    void deveRecusarEntradaNaoPositiva(int quantidade) throws Exception {
        String pecaId = cadastrar("Filtro de oleo", "35.90", 10);

        mockMvc.perform(post(PREFIXO + "/pecas/" + pecaId + "/entradas-estoque")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantidade": %d}
                                """.formatted(quantidade)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 404 ao registrar Entrada de estoque em peca inexistente")
    void deveResponder404NaEntradaDePecaInexistente() throws Exception {
        mockMvc.perform(post(PREFIXO + "/pecas/" + UUID.randomUUID() + "/entradas-estoque")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantidade": 24}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PECA_NAO_ENCONTRADA"));
    }

    @Test
    @DisplayName("deve remover logicamente, sumindo da listagem e do detalhe sem apagar o registro")
    void deveRemoverLogicamente() throws Exception {
        String pecaId = cadastrar("Filtro de oleo", "35.90", 10);

        mockMvc.perform(delete(PREFIXO + "/pecas/" + pecaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PREFIXO + "/pecas/" + pecaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PECA_NAO_ENCONTRADA"));

        mockMvc.perform(get(PREFIXO + "/pecas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        Integer inativas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM peca WHERE id = ?::uuid AND ativo = FALSE", Integer.class, pecaId);
        assertThat(inativas).isEqualTo(1);
    }

    @Test
    @DisplayName("deve responder 404 ao remover peca inexistente")
    void deveResponder404AoRemoverInexistente() throws Exception {
        mockMvc.perform(delete(PREFIXO + "/pecas/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PECA_NAO_ENCONTRADA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "EM_EXECUCAO"})
    @DisplayName("deve recusar remocao de peca em Ordem de Servico em andamento, com 409")
    void deveRecusarRemocaoComOrdemEmAndamento(String status) throws Exception {
        String pecaId = cadastrar("Filtro de oleo", "35.90", 10);
        incluirEmOrdemComStatus(pecaId, status);

        mockMvc.perform(delete(PREFIXO + "/pecas/" + pecaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PECA_COM_ORDEM_SERVICO_EM_ANDAMENTO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FINALIZADA", "ENTREGUE", "CANCELADA"})
    @DisplayName("deve permitir remocao quando a Ordem de Servico que usa a peca ja esta encerrada")
    void devePermitirRemocaoComOrdemEncerrada(String status) throws Exception {
        String pecaId = cadastrar("Filtro de oleo", "35.90", 10);
        incluirEmOrdemComStatus(pecaId, status);

        mockMvc.perform(delete(PREFIXO + "/pecas/" + pecaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("deve recusar remocao de peca com Reserva de peca ATIVA, com 409")
    void deveRecusarRemocaoComReservaAtiva() throws Exception {
        String pecaId = cadastrar("Filtro de oleo", "35.90", 10);
        registrarEntrada(pecaId, 5);
        reservarNoBanco(pecaId, "ATIVA");

        mockMvc.perform(delete(PREFIXO + "/pecas/" + pecaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PECA_COM_RESERVA_ATIVA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONSUMIDA", "DEVOLVIDA"})
    @DisplayName("deve permitir remocao de peca cujas reservas ja foram encerradas")
    void devePermitirRemocaoComReservaEncerrada(String situacao) throws Exception {
        String pecaId = cadastrar("Filtro de oleo", "35.90", 10);
        registrarEntrada(pecaId, 5);
        reservarNoBanco(pecaId, situacao);

        mockMvc.perform(delete(PREFIXO + "/pecas/" + pecaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("deve responder 400 quando o identificador do caminho nao e UUID")
    void deveResponder400ParaIdentificadorMalformado() throws Exception {
        mockMvc.perform(get(PREFIXO + "/pecas/nao-e-uuid").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "a0000000-0000-4000-8000-1",
            "0a0000000-000-4000-8000-000000000001",
            " "})
    @DisplayName("deve recusar identificador fora do formato canonico, sem resolver outra peca")
    void deveRecusarIdentificadorForaDoFormato(String identificador) throws Exception {
        mockMvc.perform(get(PREFIXO + "/pecas/{id}", identificador)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve aceitar identificador em caixa alta e resolver a peca certa")
    void deveAceitarIdentificadorEmCaixaAlta() throws Exception {
        String pecaId = cadastrar("Filtro de oleo", "35.90", 10);

        mockMvc.perform(get(PREFIXO + "/pecas/" + pecaId.toUpperCase(Locale.ROOT))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pecaId));
    }

    @Test
    @DisplayName("deve exigir JWT nas rotas de peca")
    void deveExigirAutenticacao() throws Exception {
        mockMvc.perform(get(PREFIXO + "/pecas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"));
    }
}
