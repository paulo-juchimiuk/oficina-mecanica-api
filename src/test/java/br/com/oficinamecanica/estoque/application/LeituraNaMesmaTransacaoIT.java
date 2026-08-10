package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.PendenciaPeca;
import br.com.oficinamecanica.estoque.domain.ReservaPeca;
import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reserva e leitura das pecas da Ordem de Servico dentro de uma unica transacao")
class LeituraNaMesmaTransacaoIT extends IntegracaoBase {

    private static final String NOME_DA_PECA = "Pastilha de freio dianteira";
    private static final int SALDO_DISPONIVEL = 1;
    private static final int QUANTIDADE_PEDIDA = 3;
    private static final int QUANTIDADE_FALTANTE = QUANTIDADE_PEDIDA - SALDO_DISPONIVEL;

    @Autowired
    private ReservarPecasUseCase reservarPecas;

    @Autowired
    private ConsultarPecasReservadasUseCase consultarPecasReservadas;

    @Autowired
    private ConsultarPendenciasDePecasUseCase consultarPendencias;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("deve resolver o nome da Peca ao reservar e reler as reservas sem sair da transacao")
    void deveResolverONomeDaPecaAoReservarERelerNaMesmaTransacao() {
        UUID pecaId = criarPecaComSaldo();
        UUID ordemId = criarOrdemEmExecucao();

        List<ReservaPeca> reservas = new TransactionTemplate(transactionManager).execute(status -> {
            reservarPecas.executar(ordemId, List.of(new ItemAReservar(pecaId, QUANTIDADE_PEDIDA)));
            return consultarPecasReservadas.executar(ordemId);
        });

        assertThat(reservas).singleElement()
                .satisfies(reserva -> {
                    assertThat(reserva.nomePeca()).isEqualTo(NOME_DA_PECA);
                    assertThat(reserva.quantidade()).isEqualTo(SALDO_DISPONIVEL);
                });
    }

    @Test
    @DisplayName("deve resolver o nome da Peca ao abrir pendencia e reler as pendencias sem sair da transacao")
    void deveResolverONomeDaPecaAoAbrirPendenciaERelerNaMesmaTransacao() {
        UUID pecaId = criarPecaComSaldo();
        UUID ordemId = criarOrdemEmExecucao();

        List<PendenciaPeca> pendencias = new TransactionTemplate(transactionManager).execute(status -> {
            reservarPecas.executar(ordemId, List.of(new ItemAReservar(pecaId, QUANTIDADE_PEDIDA)));
            return consultarPendencias.executar(ordemId);
        });

        assertThat(pendencias).singleElement()
                .satisfies(pendencia -> {
                    assertThat(pendencia.nomePeca()).isEqualTo(NOME_DA_PECA);
                    assertThat(pendencia.quantidadeFaltante()).isEqualTo(QUANTIDADE_FALTANTE);
                });
    }

    private String codigoDeAcompanhamento() {
        return "ACMP-" + UUID.randomUUID().toString().replace("-", "");
    }

    private UUID criarPecaComSaldo() {
        UUID pecaId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO peca (id, nome, unidade_medida, preco, moeda, saldo_em_estoque, quantidade_reservada,
                                  estoque_minimo)
                VALUES (?, ?, 'unidade', 189.90, 'BRL', ?, 0, 4)
                """, pecaId, NOME_DA_PECA, SALDO_DISPONIVEL);
        return pecaId;
    }

    private UUID criarOrdemEmExecucao() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID ordemId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                clienteId, "Ana Beatriz Souza", "10433218100", "ana@example.com");
        jdbc.update("INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id) VALUES (?, ?, ?, ?, ?, ?)",
                veiculoId, "ABC1234", "Fiat", "Uno", 2015, clienteId);
        jdbc.update("""
                INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, criada_em)
                VALUES (?, ?, ?, 'EM_EXECUCAO', ?, NOW())
                """, ordemId, clienteId, veiculoId, codigoDeAcompanhamento());
        return ordemId;
    }
}
