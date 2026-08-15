package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import br.com.oficinamecanica.estoque.application.ItemAReservar;
import br.com.oficinamecanica.estoque.application.RegistrarFaltaDePecaUseCase;
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

@DisplayName("Guards do Estoque que decidem pelo Status da OS, sob concorrencia")
class GuardDeEstadoConcorrenteIT extends IntegracaoBase {

    private static final int QUANTIDADE_FALTANTE = 2;
    private static final int RODADAS = 15;
    private static final String EM_EXECUCAO = "EM_EXECUCAO";
    private static final String DOCUMENTO = "10433218100";

    @MockitoBean
    private MailSender mailSender;

    @Autowired
    private CriarOrdemServicoUseCase criarOrdemServico;

    @Autowired
    private IniciarDiagnosticoUseCase iniciarDiagnostico;

    @Autowired
    private IncluirItensUseCase incluirItens;

    @Autowired
    private ConcluirDiagnosticoUseCase concluirDiagnostico;

    @Autowired
    private AprovarOrcamentoUseCase aprovarOrcamento;

    @Autowired
    private ConcluirExecucaoUseCase concluirExecucao;

    @Autowired
    private RegistrarFaltaDePecaUseCase registrarFaltaDePeca;

    private UUID ordemId;
    private UUID pecaId;
    private UUID veiculoId;
    private UUID servicoId;

    @BeforeEach
    void prepararCadastros() {
        UUID clienteId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();
        servicoId = UUID.randomUUID();
        pecaId = UUID.randomUUID();
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
        jdbc.update("""
                INSERT INTO peca (id, nome, unidade_medida, preco, moeda, saldo_em_estoque,
                                  quantidade_reservada, estoque_minimo)
                VALUES (?, 'Filtro de oleo', 'unidade', 50.00, 'BRL', 20, 0, 2)
                """, pecaId);
    }

    private void abrirOrdemEmExecucao() {
        OrdemServico ordem = criarOrdemServico.executar(DOCUMENTO, veiculoId, "Barulho ao frear");
        ordemId = ordem.id();
        iniciarDiagnostico.executar(ordemId);
        incluirItens.executar(ordemId, List.of(new ItemDeServicoRequisitado(servicoId)), List.of());
        concluirDiagnostico.executar(ordemId);
        aprovarOrcamento.executar(ordem.codigoAcompanhamento().valor());
    }

    @Test
    @DisplayName("nao deve aceitar registro de falta depois que a conclusao simultanea tirou a OS de execucao")
    void naoDeveNascerPendenciaParaOrdemFinalizada() throws Exception {
        for (int rodada = 1; rodada <= RODADAS; rodada++) {
            abrirOrdemEmExecucao();
            disputarConclusaoContraRegistroDeFalta(rodada);
        }
    }

    private void disputarConclusaoContraRegistroDeFalta(int rodada) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> corridas = List.of(
                executor.submit(tentativa(largada, () -> concluirExecucao.executar(ordemId))),
                executor.submit(tentativa(largada, () ->
                        registrarFaltaDePeca.executar(ordemId, pecaId, QUANTIDADE_FALTANTE))));

        largada.countDown();
        boolean conclusaoAceita = Boolean.TRUE.equals(corridas.get(0).get(60, TimeUnit.SECONDS));
        boolean faltaAceita = Boolean.TRUE.equals(corridas.get(1).get(60, TimeUnit.SECONDS));
        executor.shutdown();
        assertThat(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        String status = jdbc.queryForObject(
                "SELECT status FROM ordem_servico WHERE id = ?", String.class, ordemId);
        int pendencias = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pendencia_peca WHERE ordem_servico_id = ?", Integer.class, ordemId);

        assertThat(conclusaoAceita || faltaAceita)
                .as("rodada %d: as duas operacoes foram recusadas, e uma delas chegou primeiro", rodada)
                .isTrue();
        assertThat(pendencias > 0)
                .as("rodada %d: a Pendencia existe se e somente se o registro de falta foi aceito;"
                        + " aceito=%s, pendencias=%d", rodada, faltaAceita, pendencias)
                .isEqualTo(faltaAceita);
        assertThat(faltaAceita || !EM_EXECUCAO.equals(status))
                .as("rodada %d: registro de falta recusado e a OS permanece em %s, ou seja, a recusa nao"
                        + " tem explicacao no estado final observavel", rodada, status)
                .isTrue();
    }

    private Callable<Boolean> tentativa(CountDownLatch largada, Callable<?> operacao) {
        return () -> {
            largada.await();
            try {
                operacao.call();
                return true;
            } catch (ConflitoDeEstadoException recusada) {
                return false;
            }
        };
    }
}
