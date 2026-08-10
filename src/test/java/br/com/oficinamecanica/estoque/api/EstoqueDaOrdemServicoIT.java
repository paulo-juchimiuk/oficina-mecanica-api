package br.com.oficinamecanica.estoque.api;

import br.com.oficinamecanica.estoque.application.ItemAReservar;
import br.com.oficinamecanica.estoque.application.ReservarPecasUseCase;
import br.com.oficinamecanica.estoque.domain.ReservaPeca;
import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Reserva, Baixa de estoque, Devolucao e Pendencias por Ordem de Servico")
class EstoqueDaOrdemServicoIT extends IntegracaoBase {

    @Autowired
    private ReservarPecasUseCase reservarPecas;

    private final AtomicInteger sequencia = new AtomicInteger();

    private String token;

    @BeforeEach
    void prepararCenario() throws Exception {
        token = tokenAdministrativo();
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

    private UUID criarPecaComSaldo(String nome, int saldo) {
        UUID pecaId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO peca (id, nome, unidade_medida, preco, moeda, saldo_em_estoque, quantidade_reservada,
                                  estoque_minimo)
                VALUES (?, ?, 'unidade', 189.90, 'BRL', ?, 0, 4)
                """, pecaId, nome, saldo);
        return pecaId;
    }

    private ReservaPeca reservar(UUID ordemServicoId, UUID pecaId, int quantidade) {
        return reservarPecas.executar(ordemServicoId, List.of(new ItemAReservar(pecaId, quantidade)))
                .getFirst().reserva().orElseThrow();
    }

    private String corpoComReserva(UUID reservaId) {
        return """
                {"reservas": [{"reservaId": "%s"}]}
                """.formatted(reservaId);
    }

    private int saldoDe(UUID pecaId) {
        return jdbc.queryForObject("SELECT saldo_em_estoque FROM peca WHERE id = ?", Integer.class, pecaId);
    }

    private int reservadoDe(UUID pecaId) {
        return jdbc.queryForObject("SELECT quantidade_reservada FROM peca WHERE id = ?", Integer.class, pecaId);
    }

    @Test
    @DisplayName("deve consultar as pecas reservadas da Ordem de Servico com o nome da peca")
    void deveConsultarPecasReservadas() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemId, pecaId, 2);

        mockMvc.perform(get(PREFIXO + "/ordens-servico/" + ordemId + "/pecas-reservadas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reserva.id().toString()))
                .andExpect(jsonPath("$[0].pecaId").value(pecaId.toString()))
                .andExpect(jsonPath("$[0].nomePeca").value("Pastilha de freio dianteira"))
                .andExpect(jsonPath("$[0].quantidade").value(2))
                .andExpect(jsonPath("$[0].situacao").value("ATIVA"))
                .andExpect(jsonPath("$[0].criadaEm").isNotEmpty());
    }

    @Test
    @DisplayName("deve responder 404 ao consultar reservas de Ordem de Servico inexistente")
    void deveResponder404NaConsultaDeOrdemInexistente() throws Exception {
        mockMvc.perform(get(PREFIXO + "/ordens-servico/" + UUID.randomUUID() + "/pecas-reservadas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ORDEM_SERVICO_NAO_ENCONTRADA"));
    }

    @Test
    @DisplayName("deve reservar o disponivel e abrir pendencia do que faltou, sem estourar o saldo")
    void deveReservarParcialmenteEAbrirPendencia() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 1);

        reservarPecas.executar(ordemId, List.of(new ItemAReservar(pecaId, 2)));

        assertThat(saldoDe(pecaId)).isEqualTo(1);
        assertThat(reservadoDe(pecaId)).isEqualTo(1);
        mockMvc.perform(get(PREFIXO + "/pendencias-pecas?ordemServicoId=" + ordemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quantidadeFaltante").value(1))
                .andExpect(jsonPath("$[0].nomePeca").value("Pastilha de freio dianteira"))
                .andExpect(jsonPath("$[0].resolvidaEm").value(nullValue()));
    }

    @Test
    @DisplayName("deve registrar a Baixa de estoque na retirada, derrubando saldo e reserva")
    void deveRetirarBaixandoOSaldo() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemId, pecaId, 2);

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/retirada")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].situacao").value("CONSUMIDA"));

        assertThat(saldoDe(pecaId)).isEqualTo(8);
        assertThat(reservadoDe(pecaId)).isZero();
    }

    @Test
    @DisplayName("deve recusar retirada de reserva que ja foi consumida, com 409")
    void deveRecusarRetiradaRepetida() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemId, pecaId, 2);
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/retirada")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isOk());

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/retirada")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RESERVA_FORA_DA_SITUACAO_ESPERADA"));

        assertThat(saldoDe(pecaId)).isEqualTo(8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "FINALIZADA", "ENTREGUE",
            "CANCELADA"})
    @DisplayName("deve recusar retirada com a Ordem de Servico fora de EM_EXECUCAO, com 409")
    void deveRecusarRetiradaForaDeExecucao(String status) throws Exception {
        UUID ordemId = criarOrdemComStatus(status);
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemId, pecaId, 2);

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/retirada")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ORDEM_SERVICO_FORA_DE_EXECUCAO"));
    }

    @Test
    @DisplayName("deve responder 404 ao retirar reserva que pertence a outra Ordem de Servico")
    void deveResponder404AoRetirarReservaDeOutraOrdem() throws Exception {
        UUID ordemComReserva = criarOrdemComStatus("EM_EXECUCAO");
        UUID outraOrdem = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemComReserva, pecaId, 2);

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + outraOrdem + "/pecas/retirada")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RESERVA_NAO_ENCONTRADA"));
    }

    @Test
    @DisplayName("deve liberar a reserva ATIVA na devolucao sem mexer no Saldo em estoque")
    void deveLiberarReservaAtivaNaDevolucao() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemId, pecaId, 2);

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/devolucao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].situacao").value("DEVOLVIDA"));

        assertThat(saldoDe(pecaId)).isEqualTo(10);
        assertThat(reservadoDe(pecaId)).isZero();
    }

    @Test
    @DisplayName("deve repor o Saldo em estoque na devolucao de reserva ja CONSUMIDA")
    void deveReporSaldoNaDevolucaoDeReservaConsumida() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemId, pecaId, 2);
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/retirada")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isOk());

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/devolucao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isOk());

        assertThat(saldoDe(pecaId)).isEqualTo(10);
        assertThat(reservadoDe(pecaId)).isZero();
    }

    @Test
    @DisplayName("deve recusar devolucao repetida da mesma reserva, que criaria peca do nada")
    void deveRecusarDevolucaoRepetida() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemId, pecaId, 2);
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/devolucao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isOk());

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/devolucao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RESERVA_JA_DEVOLVIDA"));

        assertThat(saldoDe(pecaId)).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EM_EXECUCAO", "FINALIZADA", "ENTREGUE"})
    @DisplayName("deve aceitar devolucao ate a Ordem de Servico ENTREGUE, para a reserva nao ficar orfa")
    void deveAceitarDevolucaoAteEntregue(String status) throws Exception {
        UUID ordemId = criarOrdemComStatus(status);
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemId, pecaId, 2);

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/devolucao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO", "CANCELADA"})
    @DisplayName("deve recusar devolucao com a Ordem de Servico em status que nao a aceita, com 409")
    void deveRecusarDevolucaoEmStatusIncompativel(String status) throws Exception {
        UUID ordemId = criarOrdemComStatus(status);
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemId, pecaId, 2);

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/devolucao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ORDEM_SERVICO_NAO_ACEITA_DEVOLUCAO"));
    }

    @Test
    @DisplayName("deve registrar a falta de peca como pendencia, mantendo a OS em execucao")
    void deveRegistrarFaltaDePeca() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Correia dentada", 0);

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/faltas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pecaId": "%s", "quantidadeFaltante": 3}
                                """.formatted(pecaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.ordemServicoId").value(ordemId.toString()))
                .andExpect(jsonPath("$.nomePeca").value("Correia dentada"))
                .andExpect(jsonPath("$.quantidadeFaltante").value(3))
                .andExpect(jsonPath("$.detectadaEm").isNotEmpty());

        String status = jdbc.queryForObject(
                "SELECT status FROM ordem_servico WHERE id = ?", String.class, ordemId);
        assertThat(status).isEqualTo("EM_EXECUCAO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEBIDA", "AGUARDANDO_APROVACAO", "FINALIZADA", "CANCELADA"})
    @DisplayName("deve recusar registro de falta com a Ordem de Servico fora de EM_EXECUCAO, com 409")
    void deveRecusarFaltaForaDeExecucao(String status) throws Exception {
        UUID ordemId = criarOrdemComStatus(status);
        UUID pecaId = criarPecaComSaldo("Correia dentada", 0);

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/faltas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pecaId": "%s", "quantidadeFaltante": 3}
                                """.formatted(pecaId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ORDEM_SERVICO_FORA_DE_EXECUCAO"));
    }

    @Test
    @DisplayName("deve responder 404 ao registrar falta de peca inexistente")
    void deveResponder404NaFaltaDePecaInexistente() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/faltas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pecaId": "%s", "quantidadeFaltante": 3}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PECA_NAO_ENCONTRADA"));
    }

    @Test
    @DisplayName("deve filtrar as pendencias pela Ordem de Servico e entregar todas sem filtro")
    void deveFiltrarPendenciasPorOrdemServico() throws Exception {
        UUID primeira = criarOrdemComStatus("EM_EXECUCAO");
        UUID segunda = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Correia dentada", 0);
        reservarPecas.executar(primeira, List.of(new ItemAReservar(pecaId, 1)));
        reservarPecas.executar(segunda, List.of(new ItemAReservar(pecaId, 2)));

        mockMvc.perform(get(PREFIXO + "/pendencias-pecas?ordemServicoId=" + segunda)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quantidadeFaltante").value(2));

        mockMvc.perform(get(PREFIXO + "/pendencias-pecas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("deve recusar retirada com a mesma reserva repetida na requisicao, com 409 e sem mexer no saldo")
    void deveRecusarRetiradaComReservaRepetida() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemId, pecaId, 2);

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/retirada")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reservas": [{"reservaId": "%s"}, {"reservaId": "%s"}]}
                                """.formatted(reserva.id(), reserva.id())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RESERVA_FORA_DA_SITUACAO_ESPERADA"));

        assertThat(saldoDe(pecaId)).isEqualTo(10);
        assertThat(reservadoDe(pecaId)).isEqualTo(2);
    }

    @Test
    @DisplayName("deve recusar devolucao com a mesma reserva repetida na requisicao, com 409 e sem somar duas vezes")
    void deveRecusarDevolucaoComReservaRepetida() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");
        UUID pecaId = criarPecaComSaldo("Pastilha de freio dianteira", 10);
        ReservaPeca reserva = reservar(ordemId, pecaId, 2);
        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/retirada")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComReserva(reserva.id())))
                .andExpect(status().isOk());

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/devolucao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reservas": [{"reservaId": "%s"}, {"reservaId": "%s"}]}
                                """.formatted(reserva.id(), reserva.id())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RESERVA_JA_DEVOLVIDA"));

        assertThat(saldoDe(pecaId)).isEqualTo(8);
    }

    @Test
    @DisplayName("deve recusar retirada com a lista de reservas vazia, com 400")
    void deveRecusarListaDeReservasVazia() throws Exception {
        UUID ordemId = criarOrdemComStatus("EM_EXECUCAO");

        mockMvc.perform(post(PREFIXO + "/ordens-servico/" + ordemId + "/pecas/retirada")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reservas": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve exigir JWT na consulta de pendencias")
    void deveExigirAutenticacao() throws Exception {
        mockMvc.perform(get(PREFIXO + "/pendencias-pecas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"));
    }
}
