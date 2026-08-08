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

    private int contar(String consulta) {
        return jdbc.queryForObject(consulta, Integer.class);
    }
}
