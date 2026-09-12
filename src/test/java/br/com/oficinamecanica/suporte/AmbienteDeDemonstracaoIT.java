package br.com.oficinamecanica.suporte;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Ambiente de demonstracao carregado pelo arquivo de carga")
class AmbienteDeDemonstracaoIT extends IntegracaoBase {

    private static final Path ARQUIVO_DE_CARGA = Path.of("seed", "dados-demonstracao.sql");
    private static final String LOGIN_DOCUMENTADO = "admin";
    private static final String SENHA_DOCUMENTADA = "admin123";

    private String token;

    @BeforeEach
    void carregarDadosDeDemonstracao() throws IOException {
        jdbc.execute(Files.readString(ARQUIVO_DE_CARGA));
    }

    @Test
    @DisplayName("deve autenticar com a credencial que o README publica")
    void deveAutenticarComACredencialDoReadme() throws Exception {
        assertThat(autenticar(LOGIN_DOCUMENTADO, SENHA_DOCUMENTADA)).isNotBlank();
    }

    @Test
    @DisplayName("deve expor os clientes e veiculos da carga pelas rotas administrativas")
    void deveExporOsCadastrosDaCarga() throws Exception {
        token = autenticar(LOGIN_DOCUMENTADO, SENHA_DOCUMENTADA);
        int clientes = contar("SELECT COUNT(*) FROM cliente WHERE ativo = TRUE");
        int veiculos = contar("SELECT COUNT(*) FROM veiculo WHERE ativo = TRUE");
        assertThat(clientes).isPositive();
        assertThat(veiculos).isPositive();

        mockMvc.perform(get(PREFIXO + "/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(clientes));

        mockMvc.perform(get(PREFIXO + "/veiculos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(veiculos));
    }

    @Test
    @DisplayName("deve listar so a fila de atendimento, na ordem de prioridade, sem as encerradas")
    void deveListarAFilaNaOrdemDePrioridade() throws Exception {
        token = autenticar(LOGIN_DOCUMENTADO, SENHA_DOCUMENTADA);

        mockMvc.perform(get(PREFIXO + "/ordens-servico").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].status").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$[1].status").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$[2].status").value("EM_DIAGNOSTICO"))
                .andExpect(jsonPath("$[3].status").value("RECEBIDA"));
    }

    @Test
    @DisplayName("deve manter as encerradas alcancaveis pelo filtro de status e pelo identificador")
    void deveManterAsEncerradasAlcancaveis() throws Exception {
        token = autenticar(LOGIN_DOCUMENTADO, SENHA_DOCUMENTADA);
        UUID entregue = jdbc.queryForObject(
                "SELECT id FROM ordem_servico WHERE status = 'ENTREGUE'", UUID.class);

        mockMvc.perform(get(PREFIXO + "/ordens-servico?status=ENTREGUE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(entregue.toString()));

        mockMvc.perform(get(PREFIXO + "/ordens-servico/" + entregue)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    @DisplayName("deve recusar a remocao de um cliente da carga que tem Ordem de Servico em andamento")
    void deveRecusarRemocaoDeClienteComOrdemEmAndamento() throws Exception {
        token = autenticar(LOGIN_DOCUMENTADO, SENHA_DOCUMENTADA);
        UUID clienteBloqueado = jdbc.queryForObject("""
                SELECT cliente_id FROM ordem_servico
                WHERE status IN ('RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO')
                ORDER BY status, id
                LIMIT 1
                """, UUID.class);

        mockMvc.perform(delete(PREFIXO + "/clientes/" + clienteBloqueado).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_COM_ORDEM_SERVICO_EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("deve carregar uma Ordem de Servico em cada um dos sete status, que e o que o README promete")
    void deveCarregarOsSeteStatus() {
        for (String status : new String[]{"RECEBIDA", "EM_DIAGNOSTICO", "AGUARDANDO_APROVACAO",
                "EM_EXECUCAO", "FINALIZADA", "ENTREGUE", "CANCELADA"}) {
            assertThat(contar("SELECT COUNT(*) FROM ordem_servico WHERE status = '" + status + "'"))
                    .as("Ordens de Servico no status %s", status)
                    .isPositive();
        }
    }

    @Test
    @DisplayName("deve carregar o catalogo com os dois desfechos da remocao logica, tres travados e um liberado")
    void deveCarregarOsDoisDesfechosDaRemocao() {
        assertThat(contar("""
                SELECT COUNT(DISTINCT item.servico_id) FROM item_servico item
                JOIN ordem_servico ordem ON ordem.id = item.ordem_servico_id
                WHERE ordem.status IN ('RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO')
                """)).as("servicos travados por Ordem de Servico em andamento").isEqualTo(3);

        assertThat(contar("""
                SELECT COUNT(*) FROM servico
                WHERE id IN (SELECT servico_id FROM item_servico)
                AND id NOT IN (
                    SELECT item.servico_id FROM item_servico item
                    JOIN ordem_servico ordem ON ordem.id = item.ordem_servico_id
                    WHERE ordem.status IN ('RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO'))
                """)).as("servicos liberados para remocao logica").isEqualTo(1);
    }

    @Test
    @DisplayName("deve recusar a remocao de um servico da carga que consta em Ordem de Servico em andamento")
    void deveRecusarRemocaoDeServicoComOrdemEmAndamento() throws Exception {
        token = autenticar(LOGIN_DOCUMENTADO, SENHA_DOCUMENTADA);
        UUID servicoBloqueado = jdbc.queryForObject("""
                SELECT DISTINCT item.servico_id FROM item_servico item
                JOIN ordem_servico ordem ON ordem.id = item.ordem_servico_id
                WHERE ordem.status IN ('RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO')
                ORDER BY item.servico_id
                LIMIT 1
                """, UUID.class);

        mockMvc.perform(delete(PREFIXO + "/servicos/" + servicoBloqueado).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SERVICO_COM_ORDEM_SERVICO_EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("deve permitir a remocao de um servico da carga que so consta em Ordem de Servico encerrada")
    void devePermitirRemocaoDeServicoSoEmOrdemEncerrada() throws Exception {
        token = autenticar(LOGIN_DOCUMENTADO, SENHA_DOCUMENTADA);
        UUID servicoLiberado = jdbc.queryForObject("""
                SELECT servico.id FROM servico
                WHERE servico.id IN (SELECT servico_id FROM item_servico)
                AND servico.id NOT IN (
                    SELECT item.servico_id FROM item_servico item
                    JOIN ordem_servico ordem ON ordem.id = item.ordem_servico_id
                    WHERE ordem.status IN ('RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'EM_EXECUCAO'))
                ORDER BY servico.id
                LIMIT 1
                """, UUID.class);

        mockMvc.perform(delete(PREFIXO + "/servicos/" + servicoLiberado).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("deve recusar a remocao de uma peca da carga que consta em Ordem de Servico em andamento")
    void deveRecusarRemocaoDePecaComOrdemEmAndamento() throws Exception {
        token = autenticar(LOGIN_DOCUMENTADO, SENHA_DOCUMENTADA);
        UUID pecaReservada = jdbc.queryForObject("""
                SELECT DISTINCT peca_id FROM reserva_peca WHERE situacao = 'ATIVA'
                ORDER BY peca_id
                LIMIT 1
                """, UUID.class);

        mockMvc.perform(delete(PREFIXO + "/pecas/" + pecaReservada).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PECA_COM_ORDEM_SERVICO_EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("deve trazer da carga as pecas abaixo do Estoque minimo e a pendencia que alimenta a compra")
    void deveTrazerOCenarioDeReposicaoDaCarga() throws Exception {
        token = autenticar(LOGIN_DOCUMENTADO, SENHA_DOCUMENTADA);
        int abaixoDoMinimoNaCarga = contar(
                "SELECT COUNT(*) FROM peca WHERE ativo = TRUE AND saldo_em_estoque < estoque_minimo");
        assertThat(abaixoDoMinimoNaCarga).isPositive();

        mockMvc.perform(get(PREFIXO + "/pecas?abaixoDoMinimo=true").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(abaixoDoMinimoNaCarga));

        mockMvc.perform(get(PREFIXO + "/pendencias-pecas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(contar("SELECT COUNT(*) FROM pendencia_peca")))
                .andExpect(jsonPath("$[0].nomePeca").isNotEmpty());
    }

    private int contar(String consulta) {
        return jdbc.queryForObject(consulta, Integer.class);
    }
}
