package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Transicao de status")
class TransicaoStatusTest {

    @Test
    @DisplayName("deve aceitar origem nula, porque a transicao de abertura nao vem de status nenhum")
    void deveAceitarOrigemNulaNaAbertura() {
        TransicaoStatus abertura = TransicaoStatus.abertura(StatusOrdemServico.RECEBIDA);

        assertThat(abertura.deStatus()).isNull();
        assertThat(abertura.paraStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
        assertThat(abertura.dataHora()).isNotNull();
    }

    @Test
    @DisplayName("deve exigir o status de destino")
    void deveExigirStatusDeDestino() {
        assertThatThrownBy(() -> new TransicaoStatus(StatusOrdemServico.RECEBIDA, null, LocalDateTime.now()))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve exigir data e hora, porque e o insumo do Tempo medio de execucao")
    void deveExigirDataEHora() {
        assertThatThrownBy(() -> new TransicaoStatus(
                StatusOrdemServico.RECEBIDA, StatusOrdemServico.EM_DIAGNOSTICO, null))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve comparar por valor, porque a transicao e um objeto de valor")
    void deveCompararPorValor() {
        LocalDateTime agora = LocalDateTime.now();
        assertThat(new TransicaoStatus(StatusOrdemServico.RECEBIDA, StatusOrdemServico.EM_DIAGNOSTICO, agora))
                .isEqualTo(new TransicaoStatus(
                        StatusOrdemServico.RECEBIDA, StatusOrdemServico.EM_DIAGNOSTICO, agora));
    }
}
