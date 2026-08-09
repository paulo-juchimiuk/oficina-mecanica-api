package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Entidade interna Reserva de peca")
class ReservaPecaTest {

    private ReservaPeca reserva(UUID id, UUID ordemServicoId, UUID pecaId, int quantidade, SituacaoReserva situacao) {
        return new ReservaPeca(id, ordemServicoId, pecaId, "Filtro de oleo",
                quantidade, situacao, LocalDateTime.now());
    }

    @Test
    @DisplayName("deve nascer ATIVA e guardar a Ordem de Servico que a originou")
    void deveNascerAtiva() {
        ReservaPeca reserva = reserva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2, SituacaoReserva.ATIVA);

        assertThat(reserva.estaAtiva()).isTrue();
        assertThat(reserva.estaConsumida()).isFalse();
        assertThat(reserva.quantidade()).isEqualTo(2);
        assertThat(reserva.criadaEm()).isNotNull();
    }

    @Test
    @DisplayName("deve recusar reserva sem identidade")
    void deveRecusarSemIdentidade() {
        assertThatThrownBy(() -> reserva(null, UUID.randomUUID(), UUID.randomUUID(), 1, SituacaoReserva.ATIVA))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar reserva sem Ordem de Servico")
    void deveRecusarSemOrdemServico() {
        assertThatThrownBy(() -> reserva(UUID.randomUUID(), null, UUID.randomUUID(), 1, SituacaoReserva.ATIVA))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar reserva sem Peca")
    void deveRecusarSemPeca() {
        assertThatThrownBy(() -> reserva(UUID.randomUUID(), UUID.randomUUID(), null, 1, SituacaoReserva.ATIVA))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar reserva sem quantidade positiva")
    void deveRecusarQuantidadeNaoPositiva() {
        assertThatThrownBy(() -> reserva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, SituacaoReserva.ATIVA))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar reserva sem situacao")
    void deveRecusarSemSituacao() {
        assertThatThrownBy(() -> reserva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, null))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar consumo de reserva ja DEVOLVIDA")
    void deveRecusarConsumoDeReservaDevolvida() {
        ReservaPeca devolvida =
                reserva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, SituacaoReserva.DEVOLVIDA);

        assertThatThrownBy(devolvida::consumir).isInstanceOf(ReservaForaDaSituacaoEsperadaException.class);
    }

    @Test
    @DisplayName("deve aceitar devolucao de reserva CONSUMIDA")
    void deveAceitarDevolucaoDeReservaConsumida() {
        ReservaPeca consumida =
                reserva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, SituacaoReserva.CONSUMIDA);

        consumida.devolver();

        assertThat(consumida.situacao()).isEqualTo(SituacaoReserva.DEVOLVIDA);
    }
}
