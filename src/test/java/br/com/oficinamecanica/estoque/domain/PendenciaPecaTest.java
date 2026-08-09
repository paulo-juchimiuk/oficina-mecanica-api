package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Entidade interna Pendencia de peca")
class PendenciaPecaTest {

    private static final Dinheiro PRECO = new Dinheiro(new BigDecimal("189.90"), "BRL");

    @Test
    @DisplayName("deve nascer aberta, porque a resolucao ficou fora do MVP")
    void deveNascerAberta() {
        Peca peca = Peca.cadastrar("Pastilha de freio dianteira", "unidade", PRECO, 4);
        UUID ordemServicoId = UUID.randomUUID();

        PendenciaPeca pendencia = PendenciaPeca.registrar(ordemServicoId, peca, 2);

        assertThat(pendencia.ordemServicoId()).isEqualTo(ordemServicoId);
        assertThat(pendencia.pecaId()).isEqualTo(peca.id());
        assertThat(pendencia.nomePeca()).isEqualTo(peca.nome());
        assertThat(pendencia.quantidadeFaltante()).isEqualTo(2);
        assertThat(pendencia.detectadaEm()).isNotNull();
        assertThat(pendencia.resolvidaEm()).isNull();
    }

    @Test
    @DisplayName("deve recusar pendencia sem identidade")
    void deveRecusarSemIdentidade() {
        assertThatThrownBy(() -> new PendenciaPeca(null, UUID.randomUUID(), UUID.randomUUID(),
                "Filtro de oleo", 1, LocalDateTime.now(), null))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar pendencia sem Ordem de Servico")
    void deveRecusarSemOrdemServico() {
        assertThatThrownBy(() -> new PendenciaPeca(UUID.randomUUID(), null, UUID.randomUUID(),
                "Filtro de oleo", 1, LocalDateTime.now(), null))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar pendencia sem Peca")
    void deveRecusarSemPeca() {
        assertThatThrownBy(() -> new PendenciaPeca(UUID.randomUUID(), UUID.randomUUID(), null,
                "Filtro de oleo", 1, LocalDateTime.now(), null))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve recusar pendencia sem quantidade faltante positiva")
    void deveRecusarQuantidadeNaoPositiva() {
        assertThatThrownBy(() -> new PendenciaPeca(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Filtro de oleo", 0, LocalDateTime.now(), null))
                .isInstanceOf(DadosInvalidosException.class);
    }
}
