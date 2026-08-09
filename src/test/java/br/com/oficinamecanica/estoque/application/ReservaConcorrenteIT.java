package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Concorrencia na Reserva de pecas sobre o mesmo Saldo em estoque")
class ReservaConcorrenteIT extends IntegracaoBase {

    private static final int SALDO_DISPUTADO = 5;
    private static final int QUANTIDADE_POR_ORDEM = 4;
    private static final int ORDENS_CONCORRENTES = 2;

    @Autowired
    private ReservarPecasUseCase reservarPecas;

    private UUID criarOrdemEmExecucao(String placa, String documento) {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID ordemId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                clienteId, "Ana Beatriz Souza", documento, "ana@example.com");
        jdbc.update("INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id) VALUES (?, ?, ?, ?, ?, ?)",
                veiculoId, placa, "Fiat", "Uno", 2015, clienteId);
        jdbc.update("""
                INSERT INTO ordem_servico (id, cliente_id, veiculo_id, status, codigo_acompanhamento, criada_em)
                VALUES (?, ?, ?, 'EM_EXECUCAO', ?, NOW())
                """, ordemId, clienteId, veiculoId, "ACMP-" + ordemId);
        return ordemId;
    }

    private UUID criarPecaComSaldo(int saldo) {
        UUID pecaId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO peca (id, nome, unidade_medida, preco, moeda, saldo_em_estoque, quantidade_reservada,
                                  estoque_minimo)
                VALUES (?, 'Pastilha de freio dianteira', 'unidade', 189.90, 'BRL', ?, 0, 4)
                """, pecaId, saldo);
        return pecaId;
    }

    @Test
    @DisplayName("deve serializar as reservas simultaneas, sem reserva fantasma e sem estourar o saldo")
    void deveSerializarReservasSimultaneas() throws Exception {
        UUID pecaId = criarPecaComSaldo(SALDO_DISPUTADO);
        UUID primeiraOrdem = criarOrdemEmExecucao("ABC1234", "10433218100");
        UUID segundaOrdem = criarOrdemEmExecucao("XYZ9A87", "24148457142");
        List<UUID> ordens = List.of(primeiraOrdem, segundaOrdem);

        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(ORDENS_CONCORRENTES);
        List<? extends Future<?>> corridas = ordens.stream()
                .map(ordemId -> executor.submit(() -> {
                    esperarALargada(largada);
                    reservarPecas.executar(ordemId, List.of(new ItemAReservar(pecaId, QUANTIDADE_POR_ORDEM)));
                }))
                .toList();

        largada.countDown();
        for (Future<?> corrida : corridas) {
            corrida.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        int saldo = jdbc.queryForObject("SELECT saldo_em_estoque FROM peca WHERE id = ?", Integer.class, pecaId);
        int reservada = jdbc.queryForObject(
                "SELECT quantidade_reservada FROM peca WHERE id = ?", Integer.class, pecaId);
        int somaDasReservasAtivas = jdbc.queryForObject(
                "SELECT COALESCE(SUM(quantidade), 0) FROM reserva_peca WHERE peca_id = ? AND situacao = 'ATIVA'",
                Integer.class, pecaId);
        int somaDasPendencias = jdbc.queryForObject(
                "SELECT COALESCE(SUM(quantidade_faltante), 0) FROM pendencia_peca WHERE peca_id = ?",
                Integer.class, pecaId);

        assertThat(saldo).isEqualTo(SALDO_DISPUTADO);
        assertThat(reservada).isEqualTo(SALDO_DISPUTADO);
        assertThat(somaDasReservasAtivas).isEqualTo(reservada);
        assertThat(somaDasReservasAtivas + somaDasPendencias)
                .isEqualTo(ORDENS_CONCORRENTES * QUANTIDADE_POR_ORDEM);
    }

    private void esperarALargada(CountDownLatch largada) {
        try {
            largada.await();
        } catch (InterruptedException interrupcao) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupcao);
        }
    }
}
