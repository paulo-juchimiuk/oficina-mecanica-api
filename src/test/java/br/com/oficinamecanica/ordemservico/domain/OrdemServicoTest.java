package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agregado Ordem de Servico")
class OrdemServicoTest {

    private static final String RELATO = "Barulho ao frear em baixa velocidade";
    private static final int TAMANHO_MAXIMO_DO_RELATO = 1000;

    private OrdemServico ordemAberta() {
        return OrdemServico.abrir(UUID.randomUUID(), UUID.randomUUID(), RELATO);
    }

    @Test
    @DisplayName("deve nascer Recebida, com Codigo de acompanhamento e a transicao de abertura registrada")
    void deveNascerRecebida() {
        OrdemServico ordem = ordemAberta();

        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.RECEBIDA);
        assertThat(ordem.codigoAcompanhamento()).isNotNull();
        assertThat(ordem.transicoes()).singleElement().satisfies(transicao -> {
            assertThat(transicao.deStatus()).isNull();
            assertThat(transicao.paraStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
            assertThat(transicao.dataHora()).isNotNull();
        });
    }

    @Test
    @DisplayName("deve exigir Cliente e Veiculo na abertura")
    void deveExigirClienteEVeiculo() {
        assertThatThrownBy(() -> OrdemServico.abrir(null, UUID.randomUUID(), RELATO))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> OrdemServico.abrir(UUID.randomUUID(), null, RELATO))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve aceitar Ordem de Servico sem Relato do problema")
    void deveAceitarOrdemSemRelato() {
        assertThat(OrdemServico.abrir(UUID.randomUUID(), UUID.randomUUID(), "  ").relatoDoProblema()).isNull();
        assertThat(OrdemServico.abrir(UUID.randomUUID(), UUID.randomUUID(), null).relatoDoProblema()).isNull();
    }

    @Test
    @DisplayName("deve recusar Relato do problema maior que o limite do contrato")
    void deveRecusarRelatoAcimaDoLimite() {
        String longoDemais = "a".repeat(TAMANHO_MAXIMO_DO_RELATO + 1);
        assertThatThrownBy(() -> OrdemServico.abrir(UUID.randomUUID(), UUID.randomUUID(), longoDemais))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve levar a OS de Recebida a Em diagnostico e registrar a transicao")
    void deveIniciarDiagnostico() {
        OrdemServico ordem = ordemAberta();

        ordem.iniciarDiagnostico();

        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.EM_DIAGNOSTICO);
        assertThat(ordem.transicoes()).hasSize(2);
        assertThat(ordem.transicoes().getLast()).satisfies(transicao -> {
            assertThat(transicao.deStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
            assertThat(transicao.paraStatus()).isEqualTo(StatusOrdemServico.EM_DIAGNOSTICO);
        });
    }

    @Test
    @DisplayName("deve recusar iniciar o diagnostico de uma OS que ja saiu de Recebida")
    void deveRecusarIniciarDiagnosticoForaDeRecebida() {
        OrdemServico ordem = ordemAberta();
        ordem.iniciarDiagnostico();

        assertThatThrownBy(ordem::iniciarDiagnostico)
                .isInstanceOf(TransicaoInvalidaException.class)
                .hasMessageContaining(StatusOrdemServico.EM_DIAGNOSTICO.name());
    }

    @Test
    @DisplayName("deve exigir identidade, Status da OS e Codigo de acompanhamento ao reidratar do banco")
    void deveExigirCamposObrigatoriosAoReidratar() {
        UUID id = UUID.randomUUID();
        CodigoAcompanhamento codigo = CodigoAcompanhamento.gerar();
        LocalDateTime agora = LocalDateTime.now();
        List<TransicaoStatus> historico = List.of(TransicaoStatus.abertura(StatusOrdemServico.RECEBIDA));

        assertThatThrownBy(() -> new OrdemServico(null, id, id, StatusOrdemServico.RECEBIDA,
                codigo, RELATO, agora, historico))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new OrdemServico(id, id, id, null, codigo, RELATO, agora, historico))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new OrdemServico(id, id, id, StatusOrdemServico.RECEBIDA,
                null, RELATO, agora, historico))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("nao deve permitir alterar o historico de transicoes por fora do agregado")
    void naoDevePermitirAlterarOHistoricoPorFora() {
        OrdemServico ordem = ordemAberta();

        assertThatThrownBy(() -> ordem.transicoes().add(TransicaoStatus.abertura(StatusOrdemServico.ENTREGUE)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(ordem.transicoes()).hasSize(1);
    }
}
