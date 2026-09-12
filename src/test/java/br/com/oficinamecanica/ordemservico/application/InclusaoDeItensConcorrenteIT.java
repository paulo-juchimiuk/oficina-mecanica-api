package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Concorrencia na inclusao de itens da Ordem de Servico")
class InclusaoDeItensConcorrenteIT extends IntegracaoBase {

    private static final int RODADAS = 8;
    private static final int QUANTIDADE = 1;
    private static final int ITENS_POR_ORDEM = 2;

    @Autowired
    private IncluirItensUseCase incluirItens;

    @Autowired
    private RegistrarReparoAdicionalUseCase registrarReparoAdicional;

    private final AtomicInteger sequencia = new AtomicInteger();

    @Test
    @DisplayName("deve incluir itens nas duas ordens quando as listas de pecas chegam em ordens opostas")
    void deveIncluirItensComListasDePecasEmOrdensOpostas() throws Exception {
        UUID primeiraPeca = criarPeca();
        UUID segundaPeca = criarPeca();

        for (int rodada = 0; rodada < RODADAS; rodada++) {
            UUID primeiraOrdem = criarOrdemEmDiagnostico();
            UUID segundaOrdem = criarOrdemEmDiagnostico();

            emParalelo(List.of(
                    () -> incluirPecas(primeiraOrdem, primeiraPeca, segundaPeca),
                    () -> incluirPecas(segundaOrdem, segundaPeca, primeiraPeca)));

            assertThat(itensDePecaDe(primeiraOrdem)).isEqualTo(ITENS_POR_ORDEM);
            assertThat(itensDePecaDe(segundaOrdem)).isEqualTo(ITENS_POR_ORDEM);
        }
    }

    @Test
    @DisplayName("deve incluir itens nas duas ordens quando as listas de servicos chegam em ordens opostas")
    void deveIncluirItensComListasDeServicosEmOrdensOpostas() throws Exception {
        UUID primeiroServico = criarServico();
        UUID segundoServico = criarServico();

        for (int rodada = 0; rodada < RODADAS; rodada++) {
            UUID primeiraOrdem = criarOrdemEmDiagnostico();
            UUID segundaOrdem = criarOrdemEmDiagnostico();

            emParalelo(List.of(
                    () -> incluirServicos(primeiraOrdem, primeiroServico, segundoServico),
                    () -> incluirServicos(segundaOrdem, segundoServico, primeiroServico)));

            assertThat(itensDeServicoDe(primeiraOrdem)).isEqualTo(ITENS_POR_ORDEM);
            assertThat(itensDeServicoDe(segundaOrdem)).isEqualTo(ITENS_POR_ORDEM);
        }
    }

    @Test
    @DisplayName("deve registrar o reparo adicional nas duas ordens quando as listas de pecas chegam em ordens opostas")
    void deveRegistrarReparoAdicionalComListasDePecasEmOrdensOpostas() throws Exception {
        UUID primeiraPeca = criarPeca();
        UUID segundaPeca = criarPeca();

        for (int rodada = 0; rodada < RODADAS; rodada++) {
            UUID primeiraOrdem = criarOrdemEmExecucao();
            UUID segundaOrdem = criarOrdemEmExecucao();

            emParalelo(List.of(
                    () -> registrarReparo(primeiraOrdem, primeiraPeca, segundaPeca),
                    () -> registrarReparo(segundaOrdem, segundaPeca, primeiraPeca)));

            assertThat(itensDePecaDe(primeiraOrdem)).isEqualTo(ITENS_POR_ORDEM);
            assertThat(itensDePecaDe(segundaOrdem)).isEqualTo(ITENS_POR_ORDEM);
        }
    }

    private void registrarReparo(UUID ordemId, UUID primeira, UUID segunda) {
        registrarReparoAdicional.executar(ordemId, "Folga no terminal de direcao", List.of(),
                List.of(new ItemDePecaRequisitado(primeira, QUANTIDADE),
                        new ItemDePecaRequisitado(segunda, QUANTIDADE)));
    }

    private void incluirPecas(UUID ordemId, UUID primeira, UUID segunda) {
        incluirItens.executar(ordemId, List.of(),
                List.of(new ItemDePecaRequisitado(primeira, QUANTIDADE),
                        new ItemDePecaRequisitado(segunda, QUANTIDADE)));
    }

    private void incluirServicos(UUID ordemId, UUID primeiro, UUID segundo) {
        incluirItens.executar(ordemId,
                List.of(new ItemDeServicoRequisitado(primeiro), new ItemDeServicoRequisitado(segundo)),
                List.of());
    }

    private UUID criarPeca() {
        UUID pecaId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO peca (id, nome, unidade_medida, preco, moeda, saldo_em_estoque, quantidade_reservada,
                                  estoque_minimo)
                VALUES (?, 'Pastilha de freio dianteira', 'unidade', 189.90, 'BRL', 50, 0, 4)
                """, pecaId);
        return pecaId;
    }

    private UUID criarServico() {
        UUID servicoId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO servico (id, nome, descricao, valor_mao_de_obra, moeda)
                VALUES (?, 'Troca de oleo', 'Troca completa', 120.00, 'BRL')
                """, servicoId);
        return servicoId;
    }

    private UUID criarOrdemEmDiagnostico() {
        return criarOrdem("EM_DIAGNOSTICO");
    }

    private UUID criarOrdemEmExecucao() {
        return criarOrdem("EM_EXECUCAO");
    }

    private UUID criarOrdem(String status) {
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
                VALUES (?, ?, ?, ?, ?, NOW())
                """, ordemId, clienteId, veiculoId, status, "ACMP-" + ordemId.toString().replace("-", ""));
        return ordemId;
    }

    private int itensDePecaDe(UUID ordemId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM item_peca WHERE ordem_servico_id = ?", Integer.class, ordemId);
    }

    private int itensDeServicoDe(UUID ordemId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM item_servico WHERE ordem_servico_id = ?", Integer.class, ordemId);
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

    private void esperarALargada(CountDownLatch largada) {
        try {
            largada.await();
        } catch (InterruptedException interrompida) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrompida);
        }
    }
}
