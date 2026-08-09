package br.com.oficinamecanica.estoque.application;

import br.com.oficinamecanica.estoque.domain.Dinheiro;
import br.com.oficinamecanica.estoque.domain.ReservaPeca;
import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Concorrencia entre as movimentacoes de Saldo em estoque")
class MovimentacaoConcorrenteIT extends IntegracaoBase {

    private static final int SALDO_INICIAL = 10;
    private static final int QUANTIDADE_POR_RESERVA = 1;
    private static final int ENTRADA = 20;
    private static final int REPETICOES = 3;

    @Autowired
    private ReservarPecasUseCase reservarPecas;

    @Autowired
    private RetirarPecasReservadasUseCase retirarPecas;

    @Autowired
    private DevolverPecasNaoUtilizadasUseCase devolverPecas;

    @Autowired
    private RegistrarEntradaEstoqueUseCase registrarEntrada;

    @Autowired
    private AlterarPecaUseCase alterarPeca;

    private final AtomicInteger sequencia = new AtomicInteger();
    private final AtomicInteger conflitos = new AtomicInteger();

    private UUID criarOrdemEmExecucao() {
        int ordinal = sequencia.incrementAndGet();
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID ordemId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                clienteId, "Ana Beatriz Souza", "%011d".formatted(ordinal), "ana@example.com");
        jdbc.update("INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id) VALUES (?, ?, ?, ?, ?, ?)",
                veiculoId, "ABC%04d".formatted(ordinal), "Fiat", "Uno", 2015, clienteId);
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

    private ReservaPeca reservar(UUID ordemServicoId, UUID pecaId) {
        return reservarPecas.executar(ordemServicoId, List.of(new ItemAReservar(pecaId, QUANTIDADE_POR_RESERVA)))
                .getFirst().reserva().orElseThrow();
    }

    private void emParalelo(List<Runnable> tarefas) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(tarefas.size());
        List<? extends Future<?>> corridas = tarefas.stream()
                .map(tarefa -> executor.submit(() -> {
                    esperarALargada(largada);
                    tarefa.run();
                }))
                .toList();
        largada.countDown();
        for (Future<?> corrida : corridas) {
            corrida.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
    }

    private int saldoDe(UUID pecaId) {
        return jdbc.queryForObject("SELECT saldo_em_estoque FROM peca WHERE id = ?", Integer.class, pecaId);
    }

    private int reservadoDe(UUID pecaId) {
        return jdbc.queryForObject("SELECT quantidade_reservada FROM peca WHERE id = ?", Integer.class, pecaId);
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("deve registrar as duas Baixas de estoque quando as retiradas sao simultaneas")
    void deveRegistrarAsDuasBaixasSimultaneas() throws Exception {
        UUID pecaId = criarPecaComSaldo(SALDO_INICIAL);
        UUID primeiraOrdem = criarOrdemEmExecucao();
        UUID segundaOrdem = criarOrdemEmExecucao();
        ReservaPeca primeira = reservar(primeiraOrdem, pecaId);
        ReservaPeca segunda = reservar(segundaOrdem, pecaId);

        emParalelo(List.of(
                () -> retirarPecas.executar(primeiraOrdem, List.of(primeira.id())),
                () -> retirarPecas.executar(segundaOrdem, List.of(segunda.id()))));

        assertThat(saldoDe(pecaId)).isEqualTo(SALDO_INICIAL - 2 * QUANTIDADE_POR_RESERVA);
        assertThat(reservadoDe(pecaId)).isZero();
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("deve repor o saldo das duas devolucoes quando elas sao simultaneas")
    void deveReporOSaldoDasDuasDevolucoesSimultaneas() throws Exception {
        UUID pecaId = criarPecaComSaldo(SALDO_INICIAL);
        UUID primeiraOrdem = criarOrdemEmExecucao();
        UUID segundaOrdem = criarOrdemEmExecucao();
        ReservaPeca primeira = reservar(primeiraOrdem, pecaId);
        ReservaPeca segunda = reservar(segundaOrdem, pecaId);
        retirarPecas.executar(primeiraOrdem, List.of(primeira.id()));
        retirarPecas.executar(segundaOrdem, List.of(segunda.id()));

        emParalelo(List.of(
                () -> devolverPecas.executar(primeiraOrdem, List.of(primeira.id())),
                () -> devolverPecas.executar(segundaOrdem, List.of(segunda.id()))));

        assertThat(saldoDe(pecaId)).isEqualTo(SALDO_INICIAL);
        assertThat(reservadoDe(pecaId)).isZero();
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("deve repor o saldo uma unica vez quando a MESMA reserva e devolvida duas vezes ao mesmo tempo")
    void deveReporUmaVezSoNaDevolucaoSimultaneaDaMesmaReserva() throws Exception {
        UUID pecaId = criarPecaComSaldo(SALDO_INICIAL);
        UUID ordemId = criarOrdemEmExecucao();
        ReservaPeca reserva = reservar(ordemId, pecaId);
        retirarPecas.executar(ordemId, List.of(reserva.id()));

        emParalelo(List.of(
                () -> devolverTolerandoConflito(ordemId, reserva.id()),
                () -> devolverTolerandoConflito(ordemId, reserva.id())));

        assertThat(saldoDe(pecaId)).isEqualTo(SALDO_INICIAL);
        assertThat(reservadoDe(pecaId)).isZero();
        assertThat(conflitos.get()).isEqualTo(1);
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("deve baixar o saldo uma unica vez quando a MESMA reserva e retirada duas vezes ao mesmo tempo")
    void deveBaixarUmaVezSoNaRetiradaSimultaneaDaMesmaReserva() throws Exception {
        UUID pecaId = criarPecaComSaldo(SALDO_INICIAL);
        UUID ordemId = criarOrdemEmExecucao();
        ReservaPeca reserva = reservar(ordemId, pecaId);
        reservar(criarOrdemEmExecucao(), pecaId);

        emParalelo(List.of(
                () -> retirarTolerandoConflito(ordemId, reserva.id()),
                () -> retirarTolerandoConflito(ordemId, reserva.id())));

        assertThat(saldoDe(pecaId)).isEqualTo(SALDO_INICIAL - QUANTIDADE_POR_RESERVA);
        assertThat(reservadoDe(pecaId)).isEqualTo(QUANTIDADE_POR_RESERVA);
        assertThat(conflitos.get()).isEqualTo(1);
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("deve concluir as duas retiradas quando as listas chegam em ordens opostas")
    void deveConcluirRetiradasComListasEmOrdensOpostas() throws Exception {
        UUID primeiraPeca = criarPecaComSaldo(SALDO_INICIAL);
        UUID segundaPeca = criarPecaComSaldo(SALDO_INICIAL);
        UUID primeiraOrdem = criarOrdemEmExecucao();
        UUID segundaOrdem = criarOrdemEmExecucao();
        ReservaPeca daPrimeiraOrdemNaPrimeiraPeca = reservar(primeiraOrdem, primeiraPeca);
        ReservaPeca daPrimeiraOrdemNaSegundaPeca = reservar(primeiraOrdem, segundaPeca);
        ReservaPeca daSegundaOrdemNaPrimeiraPeca = reservar(segundaOrdem, primeiraPeca);
        ReservaPeca daSegundaOrdemNaSegundaPeca = reservar(segundaOrdem, segundaPeca);

        emParalelo(List.of(
                () -> retirarPecas.executar(primeiraOrdem,
                        List.of(daPrimeiraOrdemNaPrimeiraPeca.id(), daPrimeiraOrdemNaSegundaPeca.id())),
                () -> retirarPecas.executar(segundaOrdem,
                        List.of(daSegundaOrdemNaSegundaPeca.id(), daSegundaOrdemNaPrimeiraPeca.id()))));

        assertThat(saldoDe(primeiraPeca)).isEqualTo(SALDO_INICIAL - 2 * QUANTIDADE_POR_RESERVA);
        assertThat(saldoDe(segundaPeca)).isEqualTo(SALDO_INICIAL - 2 * QUANTIDADE_POR_RESERVA);
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("deve concluir as duas devolucoes quando as listas chegam em ordens opostas")
    void deveConcluirDevolucoesComListasEmOrdensOpostas() throws Exception {
        UUID primeiraPeca = criarPecaComSaldo(SALDO_INICIAL);
        UUID segundaPeca = criarPecaComSaldo(SALDO_INICIAL);
        UUID primeiraOrdem = criarOrdemEmExecucao();
        UUID segundaOrdem = criarOrdemEmExecucao();
        ReservaPeca daPrimeiraOrdemNaPrimeiraPeca = reservar(primeiraOrdem, primeiraPeca);
        ReservaPeca daPrimeiraOrdemNaSegundaPeca = reservar(primeiraOrdem, segundaPeca);
        ReservaPeca daSegundaOrdemNaPrimeiraPeca = reservar(segundaOrdem, primeiraPeca);
        ReservaPeca daSegundaOrdemNaSegundaPeca = reservar(segundaOrdem, segundaPeca);

        emParalelo(List.of(
                () -> devolverPecas.executar(primeiraOrdem,
                        List.of(daPrimeiraOrdemNaPrimeiraPeca.id(), daPrimeiraOrdemNaSegundaPeca.id())),
                () -> devolverPecas.executar(segundaOrdem,
                        List.of(daSegundaOrdemNaSegundaPeca.id(), daSegundaOrdemNaPrimeiraPeca.id()))));

        assertThat(reservadoDe(primeiraPeca)).isZero();
        assertThat(reservadoDe(segundaPeca)).isZero();
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("deve concluir as duas reservas quando as listas de itens chegam em ordens opostas")
    void deveConcluirReservasComListasEmOrdensOpostas() throws Exception {
        UUID primeiraPeca = criarPecaComSaldo(SALDO_INICIAL);
        UUID segundaPeca = criarPecaComSaldo(SALDO_INICIAL);
        UUID primeiraOrdem = criarOrdemEmExecucao();
        UUID segundaOrdem = criarOrdemEmExecucao();

        emParalelo(List.of(
                () -> reservarPecas.executar(primeiraOrdem, List.of(
                        new ItemAReservar(primeiraPeca, QUANTIDADE_POR_RESERVA),
                        new ItemAReservar(segundaPeca, QUANTIDADE_POR_RESERVA))),
                () -> reservarPecas.executar(segundaOrdem, List.of(
                        new ItemAReservar(segundaPeca, QUANTIDADE_POR_RESERVA),
                        new ItemAReservar(primeiraPeca, QUANTIDADE_POR_RESERVA)))));

        assertThat(reservadoDe(primeiraPeca)).isEqualTo(2 * QUANTIDADE_POR_RESERVA);
        assertThat(reservadoDe(segundaPeca)).isEqualTo(2 * QUANTIDADE_POR_RESERVA);
    }

    @RepeatedTest(REPETICOES)
    @DisplayName("deve preservar a Entrada de estoque quando o cadastro da peca e alterado ao mesmo tempo")
    void devePreservarAEntradaConcorrenteComAAlteracaoDeCadastro() throws Exception {
        UUID pecaId = criarPecaComSaldo(SALDO_INICIAL);
        Dinheiro preco = new Dinheiro(new BigDecimal("189.90"), "BRL");

        emParalelo(List.of(
                () -> alterarPeca.executar(pecaId, "Pastilha renomeada", "unidade", preco, 4),
                () -> registrarEntrada.executar(pecaId, ENTRADA)));

        assertThat(saldoDe(pecaId)).isEqualTo(SALDO_INICIAL + ENTRADA);
        String nome = jdbc.queryForObject("SELECT nome FROM peca WHERE id = ?", String.class, pecaId);
        assertThat(nome).isEqualTo("Pastilha renomeada");
    }

    private void devolverTolerandoConflito(UUID ordemServicoId, UUID reservaId) {
        try {
            devolverPecas.executar(ordemServicoId, List.of(reservaId));
        } catch (ConflitoDeEstadoException conflito) {
            conflitos.incrementAndGet();
        }
    }

    private void retirarTolerandoConflito(UUID ordemServicoId, UUID reservaId) {
        try {
            retirarPecas.executar(ordemServicoId, List.of(reservaId));
        } catch (ConflitoDeEstadoException conflito) {
            conflitos.incrementAndGet();
        }
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
