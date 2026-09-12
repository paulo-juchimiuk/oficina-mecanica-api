package br.com.oficinamecanica.ordemservico.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import static br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico.AGUARDANDO_APROVACAO;
import static br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico.CANCELADA;
import static br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico.EM_DIAGNOSTICO;
import static br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico.EM_EXECUCAO;
import static br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico.ENTREGUE;
import static br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico.FINALIZADA;
import static br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico.RECEBIDA;
import static java.util.Comparator.comparingInt;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Status da OS e a maquina de estados")
class StatusOrdemServicoTest {

    @Test
    @DisplayName("deve declarar exatamente os sete Status da OS, na ordem do fluxo")
    void deveDeclararExatamenteOsSeteStatus() {
        assertThat(StatusOrdemServico.values()).containsExactly(
                RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, EM_EXECUCAO, FINALIZADA, ENTREGUE, CANCELADA);
    }

    @Test
    @DisplayName("nao deve existir status agregador, porque em andamento e criterio de consulta e nao Status da OS")
    void naoDeveExistirStatusAgregador() {
        assertThat(Arrays.stream(StatusOrdemServico.values()).map(Enum::name))
                .doesNotContain("EM_ANDAMENTO", "AGUARDANDO_PECAS");
    }

    @Test
    @DisplayName("deve aceitar apenas as transicoes que a maquina de estados desenha")
    void deveAceitarApenasAsTransicoesDaMaquinaDeEstados() {
        assertThat(destinosDe(RECEBIDA)).containsExactly(EM_DIAGNOSTICO);
        assertThat(destinosDe(EM_DIAGNOSTICO)).containsExactly(AGUARDANDO_APROVACAO);
        assertThat(destinosDe(AGUARDANDO_APROVACAO)).containsExactlyInAnyOrder(EM_EXECUCAO, CANCELADA);
        assertThat(destinosDe(EM_EXECUCAO)).containsExactlyInAnyOrder(AGUARDANDO_APROVACAO, FINALIZADA);
        assertThat(destinosDe(FINALIZADA)).containsExactly(ENTREGUE);
    }

    @Test
    @DisplayName("deve tratar Entregue e Cancelada como desfechos terminais")
    void deveTratarEntregueECanceladaComoTerminais() {
        assertThat(destinosDe(ENTREGUE)).isEmpty();
        assertThat(destinosDe(CANCELADA)).isEmpty();
    }

    @Test
    @DisplayName("deve declarar como em andamento exatamente os quatro Status de atendimento vivo")
    void deveDeclararOsQuatroStatusEmAndamento() {
        assertThat(StatusOrdemServico.emAndamento())
                .containsExactlyInAnyOrder(RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, EM_EXECUCAO);
    }

    @Test
    @DisplayName("nao deve tratar Finalizada, Entregue nem Cancelada como atendimento em andamento")
    void naoDeveTratarOsDesfechosComoEmAndamento() {
        assertThat(StatusOrdemServico.emAndamento()).doesNotContain(FINALIZADA, ENTREGUE, CANCELADA);
    }

    @Test
    @DisplayName("deve aceitar devolucao de pecas em Em execucao, Finalizada e Entregue, e so nesses tres")
    void deveDeclararOsStatusQueAceitamDevolucao() {
        assertThat(StatusOrdemServico.queAceitamDevolucaoDePecas())
                .containsExactlyInAnyOrder(EM_EXECUCAO, FINALIZADA, ENTREGUE);
    }

    @Test
    @DisplayName("nao deve permitir voltar de Finalizada para Em execucao")
    void naoDevePermitirVoltarDeFinalizadaParaEmExecucao() {
        assertThat(FINALIZADA.aceitaTransicaoPara(EM_EXECUCAO)).isFalse();
    }

    @Test
    @DisplayName("deve declarar Finalizada, Entregue e Cancelada como encerrados, e so esses tres")
    void deveDeclararOsStatusEncerrados() {
        assertThat(StatusOrdemServico.encerrados())
                .containsExactlyInAnyOrder(FINALIZADA, ENTREGUE, CANCELADA);
    }

    @Test
    @DisplayName("deve priorizar a fila de Em execucao a Recebida, com os encerrados no fim")
    void devePriorizarAFilaDeAtendimento() {
        List<StatusOrdemServico> porPrioridade = EnumSet.allOf(StatusOrdemServico.class).stream()
                .sorted(comparingInt(StatusOrdemServico::prioridadeNaFila))
                .toList();

        assertThat(porPrioridade).startsWith(EM_EXECUCAO, AGUARDANDO_APROVACAO, EM_DIAGNOSTICO, RECEBIDA);
        assertThat(porPrioridade.subList(4, porPrioridade.size()))
                .containsExactlyInAnyOrder(FINALIZADA, ENTREGUE, CANCELADA);
    }

    private List<StatusOrdemServico> destinosDe(StatusOrdemServico origem) {
        return EnumSet.allOf(StatusOrdemServico.class).stream()
                .filter(origem::aceitaTransicaoPara)
                .toList();
    }
}
