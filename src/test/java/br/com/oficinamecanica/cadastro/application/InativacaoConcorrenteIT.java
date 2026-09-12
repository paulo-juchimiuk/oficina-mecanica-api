package br.com.oficinamecanica.cadastro.application;

import br.com.oficinamecanica.cadastro.domain.Contato;
import br.com.oficinamecanica.cadastro.domain.Documento;
import br.com.oficinamecanica.cadastro.domain.Placa;
import br.com.oficinamecanica.catalogo.application.AlterarServicoUseCase;
import br.com.oficinamecanica.catalogo.application.InativarServicoUseCase;
import br.com.oficinamecanica.catalogo.domain.Dinheiro;
import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import br.com.oficinamecanica.shared.domain.RecursoNaoEncontradoException;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Inativacao de cadastro disputando com o uso e com a alteracao do registro")
class InativacaoConcorrenteIT extends IntegracaoBase {

    private static final int RODADAS = 10;
    private static final String DOCUMENTO = "10433218100";
    private static final List<String> DOCUMENTOS_DAS_RODADAS = List.of(
            "00000000191", "00000000272", "00000000353", "00000000434", "00000000515",
            "00000000604", "00000000787", "00000000868", "00000000949", "00000001082");
    private static final List<String> DOCUMENTOS_DO_NOVO_DONO = List.of(
            "00000001163", "00000001244", "00000001325", "00000001406", "00000001597",
            "00000001678", "00000001759", "00000001830", "00000001910", "00000002054");
    private static final int ESPERA_EM_SEGUNDOS = 60;
    private static final int VIAS_EM_DISPUTA = 12;
    private static final String MOEDA = "BRL";

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

    @Autowired
    private AlterarClienteUseCase alterarCliente;

    @Autowired
    private InativarVeiculoUseCase inativarVeiculo;

    @Autowired
    private AlterarVeiculoUseCase alterarVeiculo;

    @Autowired
    private InativarServicoUseCase inativarServico;

    @Autowired
    private AlterarServicoUseCase alterarServico;

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
            Documento documentoDaRodada = documentoDaRodada(rodada);
            UUID clienteDaRodada = inserirCliente(rodada, documentoDaRodada);
            UUID veiculoDaRodada = inserirVeiculo(rodada, clienteDaRodada);
            String relato = "corrida " + rodada;

            int aceitas = disputar(
                    () -> criarOrdemServico.executar(documentoDaRodada.numero(), veiculoDaRodada, relato, List.of(), List.of()),
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

    @Test
    @DisplayName("nao deve ressuscitar o Cliente removido quando a alteracao corre na mesma janela")
    void naoDeveRessuscitarClienteAlteradoNaMesmaCorrida() throws Exception {
        for (int rodada = 1; rodada <= RODADAS; rodada++) {
            Documento documento = documentoDaRodada(rodada);
            UUID id = inserirCliente(rodada, documento);
            Contato contato = new Contato("corrida%d@example.com".formatted(rodada), null);
            String nomeAlterado = "Cliente alterado na corrida " + rodada;

            disputar(
                    () -> {
                        inativarCliente.executar(id);
                        return null;
                    },
                    () -> alterarCliente.executar(id, nomeAlterado, documento, contato));

            boolean ativo = Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT ativo FROM cliente WHERE id = ?", Boolean.class, id));

            assertThat(ativo)
                    .as("rodada %d: a alteracao gravou por cima da remocao e o Cliente voltou a existir;"
                            + " nao ha reativacao no MVP (ADR-014)", rodada)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("nao deve ressuscitar o Veiculo removido quando a alteracao corre na mesma janela")
    void naoDeveRessuscitarVeiculoAlteradoNaMesmaCorrida() throws Exception {
        for (int rodada = 1; rodada <= RODADAS; rodada++) {
            UUID donoId = inserirCliente(rodada, documentoDaRodada(rodada));
            UUID id = inserirVeiculo(rodada, donoId);
            Placa placa = new Placa(placaDaRodada(rodada));

            disputar(
                    () -> {
                        inativarVeiculo.executar(id);
                        return null;
                    },
                    () -> alterarVeiculo.executar(id, placa, "Volkswagen", "Gol 1.6", 2021, donoId));

            boolean ativo = Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT ativo FROM veiculo WHERE id = ?", Boolean.class, id));

            assertThat(ativo)
                    .as("rodada %d: a alteracao gravou por cima da remocao e o Veiculo voltou a existir;"
                            + " nao ha reativacao no MVP (ADR-014)", rodada)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("nao deve ressuscitar o Servico removido quando a alteracao corre na mesma janela")
    void naoDeveRessuscitarServicoAlteradoNaMesmaCorrida() throws Exception {
        for (int rodada = 1; rodada <= RODADAS; rodada++) {
            UUID id = inserirServico(rodada);
            Dinheiro valorMaoDeObra = new Dinheiro(new BigDecimal("130.00"), MOEDA);
            String nomeAlterado = "Servico alterado na corrida " + rodada;

            disputar(
                    () -> {
                        inativarServico.executar(id);
                        return null;
                    },
                    () -> alterarServico.executar(id, nomeAlterado, "Descricao alterada", valorMaoDeObra));

            boolean ativo = Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT ativo FROM servico WHERE id = ?", Boolean.class, id));

            assertThat(ativo)
                    .as("rodada %d: a alteracao gravou por cima da remocao e o Servico voltou a existir;"
                            + " nao ha reativacao no MVP (ADR-014)", rodada)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("nao deve entrar em impasse quando a troca de dono corre com a criacao da OS")
    void naoDeveEntrarEmImpasseNaTrocaDeDono() throws Exception {
        Documento documentoDoDono = documentoDaRodada(1);
        UUID donoAtual = inserirCliente(1, documentoDoDono);
        UUID novoDono = inserirCliente(1 + RODADAS, documentoDoNovoDono(1));
        UUID veiculoDaRodada = inserirVeiculo(1, donoAtual);
        Placa placa = new Placa(placaDaRodada(1));

        List<Callable<Boolean>> alteracoes = new ArrayList<>();
        List<Callable<Boolean>> criacoes = new ArrayList<>();
        CountDownLatch largada = new CountDownLatch(1);
        for (int via = 1; via <= VIAS_EM_DISPUTA; via++) {
            UUID dono = via % 2 == 0 ? donoAtual : novoDono;
            String relato = "corrida " + via;
            alteracoes.add(tentativa(largada,
                    () -> alterarVeiculo.executar(veiculoDaRodada, placa, "Volkswagen", "Gol", 2021, dono)));
            criacoes.add(tentativa(largada,
                    () -> criarOrdemServico.executar(documentoDoDono.numero(), veiculoDaRodada, relato, List.of(), List.of())));
        }

        ExecutorService executor = Executors.newFixedThreadPool(alteracoes.size() + criacoes.size());
        List<Future<Boolean>> corridasDeAlteracao = alteracoes.stream().map(executor::submit).toList();
        List<Future<Boolean>> corridasDeCriacao = criacoes.stream().map(executor::submit).toList();
        largada.countDown();

        List<String> impasses = new ArrayList<>();
        int alteracoesAceitas = contarAceitas(corridasDeAlteracao, impasses);
        contarAceitas(corridasDeCriacao, impasses);
        executor.shutdown();
        assertThat(executor.awaitTermination(ESPERA_EM_SEGUNDOS, TimeUnit.SECONDS)).isTrue();

        assertThat(impasses)
                .as("toda recusa tem de vir de regra de negocio; falha de banco aqui e impasse por ordem"
                        + " de travas invertida, e a ordem declarada no ADR-020 e Cliente antes de Veiculo")
                .isEmpty();
        assertThat(alteracoesAceitas)
                .as("nenhuma alteracao de veiculo foi aceita, entao a disputa nao aconteceu e a ausencia"
                        + " de impasse nao prova nada; a criacao da OS pode ser recusada por regra, porque"
                        + " a troca de dono a torna invalida, e por isso ela nao entra nesta asercao")
                .isGreaterThanOrEqualTo(1);
    }

    private int contarAceitas(List<Future<Boolean>> corridas, List<String> impasses) throws Exception {
        int aceitas = 0;
        for (Future<Boolean> corrida : corridas) {
            try {
                aceitas += Boolean.TRUE.equals(corrida.get(ESPERA_EM_SEGUNDOS, TimeUnit.SECONDS)) ? 1 : 0;
            } catch (ExecutionException falha) {
                impasses.add(String.valueOf(falha.getCause()));
            }
        }
        return aceitas;
    }

    private Documento documentoDoNovoDono(int rodada) {
        return new Documento(DOCUMENTOS_DO_NOVO_DONO.get(rodada - 1));
    }

    private Documento documentoDaRodada(int rodada) {
        return new Documento(DOCUMENTOS_DAS_RODADAS.get(rodada - 1));
    }

    private String placaDaRodada(int rodada) {
        return "COR%04d".formatted(rodada);
    }

    private UUID inserirCliente(int rodada, Documento documento) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                id, "Cliente da corrida " + rodada, documento.numero(), "cliente%d@example.com".formatted(rodada));
        return id;
    }

    private UUID inserirVeiculo(int rodada, UUID donoId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id)
                VALUES (?, ?, 'Volkswagen', 'Gol', 2020, ?)
                """, id, placaDaRodada(rodada), donoId);
        return id;
    }

    private UUID inserirServico(int rodada) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO servico (id, nome, descricao, valor_mao_de_obra, moeda)
                VALUES (?, ?, 'Servico da corrida', 120.00, ?)
                """, id, "Alinhamento da corrida " + rodada, MOEDA);
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
        OrdemServico ordem = criarOrdemServico.executar(DOCUMENTO, veiculoId, "corrida " + rodada, List.of(), List.of());
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
            } catch (ConflitoDeEstadoException | RecursoNaoEncontradoException recusada) {
                return false;
            }
        };
    }
}
