package br.com.oficinamecanica.ordemservico.api;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Tempo medio de execucao, calculado das Transicoes de status da carga de demonstracao")
class TempoMedioExecucaoIT extends IntegracaoBase {

    private static final Path ARQUIVO_DE_CARGA = Path.of("seed", "dados-demonstracao.sql");
    private static final String ROTA = "/ordens-servico/metricas/tempo-medio-execucao";
    private static final String TROCA_DE_OLEO = "f0000000-0000-4000-8000-000000000001";
    private static final String ALINHAMENTO = "f0000000-0000-4000-8000-000000000002";
    private static final String REVISAO_DE_FREIOS = "f0000000-0000-4000-8000-000000000003";
    private static final String DIAGNOSTICO_ELETRONICO = "f0000000-0000-4000-8000-000000000004";

    private String token;

    @BeforeEach
    void carregarDadosDeDemonstracao() throws Exception {
        jdbc.execute(Files.readString(ARQUIVO_DE_CARGA));
        token = autenticar("admin", "admin123");
    }

    @Test
    @DisplayName("deve considerar so as OSs que chegaram a Finalizada e devolver o recorte declarado no ADR-008")
    void deveConsiderarSoAsFinalizadas() throws Exception {
        consultar("")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeOSsConsideradas").value(2))
                .andExpect(jsonPath("$.recorte").value("EM_EXECUCAO_A_FINALIZADA"))
                .andExpect(jsonPath("$.servicoId").value(nullValue()));
    }

    @Test
    @DisplayName("deve tirar a media por Ordem de Servico das somas de segmentos, e nao do total corrido")
    void deveTirarAMediaPorOrdemServico() throws Exception {
        consultar("")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempoMedioHoras").value(5.0));
    }

    @Test
    @DisplayName("deve medir 5,5 horas na OS de segmento unico, filtrando pelo Servico que so ela tem")
    void deveMedirOSegmentoUnico() throws Exception {
        consultar("?servicoId=" + TROCA_DE_OLEO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeOSsConsideradas").value(1))
                .andExpect(jsonPath("$.tempoMedioHoras").value(5.5))
                .andExpect(jsonPath("$.servicoId").value(TROCA_DE_OLEO));
    }

    @Test
    @DisplayName("deve SOMAR os segmentos da OS que voltou para Aguardando aprovacao, sem a espera do Cliente")
    void deveSomarOsSegmentosDaOrdemComReparoAdicional() throws Exception {
        consultar("?servicoId=" + ALINHAMENTO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeOSsConsideradas").value(1))
                .andExpect(jsonPath("$.tempoMedioHoras").value(4.5));
    }

    @Test
    @DisplayName("deve devolver a media das OSs que INCLUEM o Servico, e nao o tempo do Servico isolado")
    void deveDevolverAMediaDasOrdensQueIncluemOServico() throws Exception {
        consultar("?servicoId=" + ALINHAMENTO)
                .andExpect(jsonPath("$.tempoMedioHoras").value(4.5));
        consultar("?servicoId=" + DIAGNOSTICO_ELETRONICO)
                .andExpect(jsonPath("$.quantidadeOSsConsideradas").value(1))
                .andExpect(jsonPath("$.tempoMedioHoras").value(4.5));
    }

    @Test
    @DisplayName("deve devolver tempo medio nulo quando nenhuma OS concluida inclui o Servico")
    void deveDevolverNuloSemOrdensNoRecorte() throws Exception {
        consultar("?servicoId=" + REVISAO_DE_FREIOS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeOSsConsideradas").value(0))
                .andExpect(jsonPath("$.tempoMedioHoras").value(nullValue()));
    }

    @Test
    @DisplayName("deve responder 400 quando o filtro por Servico nao e um identificador valido")
    void deveResponder400ComFiltroInvalido() throws Exception {
        consultar("?servicoId=nao-e-uuid")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("deve responder 401 na metrica sem token administrativo")
    void deveResponder401SemToken() throws Exception {
        mockMvc.perform(get(PREFIXO + ROTA)).andExpect(status().isUnauthorized());
    }

    private ResultActions consultar(String filtro) throws Exception {
        return mockMvc.perform(get(PREFIXO + ROTA + filtro).header("Authorization", "Bearer " + token));
    }
}
