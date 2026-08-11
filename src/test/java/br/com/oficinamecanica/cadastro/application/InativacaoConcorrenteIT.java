package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.estoque.application.InativarPecaUseCase;
import br.com.oficinamecanica.ordemservico.application.CriarOrdemServicoUseCase;
import br.com.oficinamecanica.ordemservico.application.IncluirItensUseCase;
import br.com.oficinamecanica.ordemservico.application.IniciarDiagnosticoUseCase;
import br.com.oficinamecanica.ordemservico.application.ItemDePecaRequisitado;
import br.com.oficinamecanica.ordemservico.application.ItemDeServicoRequisitado;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Inativacao de cadastro disputando com o uso do registro pela Ordem de Servico")
class InativacaoConcorrenteIT extends IntegracaoBase {

    private static final int RODADAS = 10;
    private static final String DOCUMENTO = "10433218100";
    private static final int ESPERA_EM_SEGUNDOS = 60;

    @MockitoBean
    private MailSender mailSender;

    @Autowired
    private CriarOrdemServicoUseCase criarOrdemServico;

    @Autowired
    private IniciarDiagnosticoUseCase iniciarDiagnostico;

    @Autowired
    private IncluirItensUseCase incluirItens;

    @Autowired
    private InativarPecaUseCase inativarPeca;

    @Autowired
    private InativarClienteUseCase inativarCliente;

    private UUID clienteId;
    private UUID veiculoId;
    private UUID servicoId;

    @BeforeEach
    void prepararCadastros() {
        clienteId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
        servicoId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                clienteId, "Ana Beatriz Souza", DOCUMENTO, "ana@example.com");
        jdbc.update("""
                INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id)
                VALUES (?, 'ABC1D23', 'Volkswagen', 'Gol', 2020, ?)
                """, veiculoId, clienteId);
        jdbc.update("""
                INSERT INTO servico (id, nome, descricao, valor_mao_de_obra, moeda)
                VALUES (?, 'Troca de oleo', 'Troca completa', 120.00, 'BRL')
                """, servicoId);
    }

    @Test
    @DisplayName("nao deve inativar a Peca e gravar item que a referencia na mesma corrida")
    void naoDeveInativarPecaUsadaPelaMesmaOrdemServico() throws Exception {
        for (int rodada = 1; rodada <= RODADAS; rodada++) {
            UUID pecaId = inserirPeca(rodada);
            UUID ordemId = abrirOrdemEmDiagnostico(rodada);

            int aceitas = disputar(
                    () -> incluirItens.executar(ordemId, List.of(), List.of(new ItemDePecaRequisitado(pecaId, 1))),
                    () -> {
                        inativarPeca.executar(pecaId);
                        return null;
                    });

            boolean ativa = Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT ativo FROM peca WHERE id = ?", Boolean.class, pecaId));
            int itens = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM item_peca WHERE peca_id = ?", Integer.class, pecaId);

            assertThat(aceitas).as("rodada %d: uma das duas tem de perder a corrida", rodada).isEqualTo(1);
            assertThat(ativa || itens == 0)
                    .as("rodada %d: Peca inativa com %d item de orcamento apontando para ela, estado que o"
                            + " guard do ADR-014 existe para impedir", rodada, itens)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("nao deve inativar o Cliente e abrir OS para ele na mesma corrida, o que deixaria a OS presa")
    void naoDeveInativarClienteComOrdemServicoNascendo() throws Exception {
        for (int rodada = 1; rodada <= RODADAS; rodada++) {
            String documentoDaRodada = "%011d".formatted(rodada);
            UUID clienteDaRodada = inserirCliente(rodada, documentoDaRodada);
            UUID veiculoDaRodada = inserirVeiculo(rodada, clienteDaRodada);
            String relato = "corrida " + rodada;

            int aceitas = disputar(
                    () -> criarOrdemServico.executar(documentoDaRodada, veiculoDaRodada, relato),
                    () -> {
                        inativarCliente.executar(clienteDaRodada);
                        return null;
                    });

            boolean ativo = Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT ativo FROM cliente WHERE id = ?", Boolean.class, clienteDaRodada));

            assertThat(aceitas).as("rodada %d: uma das duas tem de perder a corrida", rodada).isEqualTo(1);
            assertThat(ativo || ordensDe(clienteDaRodada) == 0)
                    .as("rodada %d: Cliente inativado com Ordem de Servico nascendo; a OS nao avanca por"
                            + " nenhuma rota e nao existe reativacao (ADR-014)", rodada)
                    .isTrue();
        }
    }

    private UUID inserirCliente(int rodada, String documento) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                id, "Cliente da corrida " + rodada, documento, "cliente%d@example.com".formatted(rodada));
        return id;
    }

    private UUID inserirVeiculo(int rodada, UUID donoId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id)
                VALUES (?, ?, 'Volkswagen', 'Gol', 2020, ?)
                """, id, "COR%04d".formatted(rodada), donoId);
        return id;
    }

    private int ordensDe(UUID donoId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM ordem_servico WHERE cliente_id = ?", Integer.class, donoId);
    }

    private UUID inserirPeca(int rodada) {
        UUID pecaId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO peca (id, nome, unidade_medida, preco, moeda, saldo_em_estoque,
                                  quantidade_reservada, estoque_minimo)
                VALUES (?, ?, 'unidade', 50.00, 'BRL', 20, 0, 2)
                """, pecaId, "Filtro de oleo " + rodada);
        return pecaId;
    }

    private UUID abrirOrdemEmDiagnostico(int rodada) {
        OrdemServico ordem = criarOrdemServico.executar(DOCUMENTO, veiculoId, "corrida " + rodada);
        iniciarDiagnostico.executar(ordem.id());
        incluirItens.executar(ordem.id(), List.of(new ItemDeServicoRequisitado(servicoId)), List.of());
        return ordem.id();
    }

    private int disputar(Callable<?> primeira, Callable<?> segunda) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> corridas = List.of(
                executor.submit(tentativa(largada, primeira)),
                executor.submit(tentativa(largada, segunda)));

        largada.countDown();
        int aceitas = 0;
        for (Future<Boolean> corrida : corridas) {
            aceitas += Boolean.TRUE.equals(corrida.get(ESPERA_EM_SEGUNDOS, TimeUnit.SECONDS)) ? 1 : 0;
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(ESPERA_EM_SEGUNDOS, TimeUnit.SECONDS)).isTrue();
        return aceitas;
    }

    private Callable<Boolean> tentativa(CountDownLatch largada, Callable<?> operacao) {
        return () -> {
            largada.await();
            try {
                operacao.call();
                return true;
            } catch (RuntimeException recusada) {
                return false;
            }
        };
    }
}
