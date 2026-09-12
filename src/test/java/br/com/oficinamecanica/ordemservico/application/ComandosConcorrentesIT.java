package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Concorrencia nos comandos administrativos da Ordem de Servico")
class ComandosConcorrentesIT extends IntegracaoBase {

    private static final String ASSUNTO_DO_ORCAMENTO = "Orcamento da sua Ordem de Servico";

    private static final int SIMULTANEAS = 4;
    private static final String VALOR_DO_SERVICO = "120.00";

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
    private RegistrarEntregaUseCase registrarEntrega;

    private UUID servicoId;
    private UUID ordemId;

    @BeforeEach
    void prepararOrdemRecebida() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        servicoId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                clienteId, "Ana Beatriz Souza", "10433218100", "ana@example.com");
        jdbc.update("""
                INSERT INTO veiculo (id, placa, marca, modelo, ano, cliente_id)
                VALUES (?, 'ABC1D23', 'Volkswagen', 'Gol', 2020, ?)
                """, veiculoId, clienteId);
        jdbc.update("""
                INSERT INTO servico (id, nome, descricao, valor_mao_de_obra, moeda)
                VALUES (?, 'Troca de oleo', 'Troca completa', %s, 'BRL')
                """.formatted(VALOR_DO_SERVICO), servicoId);
        ordemId = criarOrdemServico.executar("10433218100", veiculoId, "Barulho ao frear", List.of(), List.of()).id();
    }

    @Test
    @DisplayName("deve iniciar o diagnostico uma unica vez, sem duplicar a transicao no historico do Cliente")
    void deveIniciarODiagnosticoUmaUnicaVez() throws Exception {
        assertThat(disparar(() -> iniciarDiagnostico.executar(ordemId))).isEqualTo(1);
        assertThat(transicoesPara("EM_DIAGNOSTICO")).isEqualTo(1);
    }

    @Test
    @DisplayName("deve manter o total do Orcamento igual a soma dos itens quando as inclusoes correm juntas")
    void deveManterOTotalIgualASomaDosItens() throws Exception {
        iniciarDiagnostico.executar(ordemId);

        int aceitas = disparar(() -> incluirItens.executar(ordemId,
                List.of(new ItemDeServicoRequisitado(servicoId)), List.of()));

        assertThat(aceitas).as("incluir itens nao transiciona status, entao as quatro sao legitimas")
                .isEqualTo(SIMULTANEAS);
        assertThat(contarItens()).isEqualTo(SIMULTANEAS);
        assertThat(totalGravado())
                .isEqualByComparingTo(new BigDecimal(VALOR_DO_SERVICO).multiply(BigDecimal.valueOf(SIMULTANEAS)));
    }

    @Test
    @DisplayName("deve concluir o diagnostico uma unica vez, enviando um unico e-mail ao Cliente")
    void deveConcluirODiagnosticoUmaUnicaVez() throws Exception {
        iniciarDiagnostico.executar(ordemId);
        incluirItens.executar(ordemId, List.of(new ItemDeServicoRequisitado(servicoId)), List.of());

        assertThat(disparar(() -> concluirDiagnostico.executar(ordemId))).isEqualTo(1);
        assertThat(transicoesPara("AGUARDANDO_APROVACAO")).isEqualTo(1);
        assertThat(emailsComAssunto(ASSUNTO_DO_ORCAMENTO)).hasSize(1);
    }

    @Test
    @DisplayName("deve concluir a execucao e registrar a entrega uma unica vez cada")
    void deveConcluirEEntregarUmaUnicaVez() throws Exception {
        iniciarDiagnostico.executar(ordemId);
        incluirItens.executar(ordemId, List.of(new ItemDeServicoRequisitado(servicoId)), List.of());
        concluirDiagnostico.executar(ordemId);
        aprovarOrcamento.executar(codigoDeAcompanhamento());

        assertThat(disparar(() -> concluirExecucao.executar(ordemId))).isEqualTo(1);
        assertThat(transicoesPara("FINALIZADA")).isEqualTo(1);

        assertThat(disparar(() -> registrarEntrega.executar(ordemId))).isEqualTo(1);
        assertThat(transicoesPara("ENTREGUE")).isEqualTo(1);
    }

    private int disparar(Callable<?> operacao) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(SIMULTANEAS);
        List<Future<Boolean>> corridas = IntStream.range(0, SIMULTANEAS)
                .mapToObj(vez -> executor.submit(tentativa(largada, operacao)))
                .toList();

        largada.countDown();
        int aceitas = 0;
        for (Future<Boolean> corrida : corridas) {
            aceitas += Boolean.TRUE.equals(corrida.get(60, TimeUnit.SECONDS)) ? 1 : 0;
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        return aceitas;
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

    private String codigoDeAcompanhamento() {
        return jdbc.queryForObject(
                "SELECT codigo_acompanhamento FROM ordem_servico WHERE id = ?", String.class, ordemId);
    }

    private int transicoesPara(String status) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM transicao_status
                WHERE ordem_servico_id = ? AND para_status = ?
                """, Integer.class, ordemId, status);
    }

    private int contarItens() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM item_servico WHERE ordem_servico_id = ?", Integer.class, ordemId);
    }

    private BigDecimal totalGravado() {
        return jdbc.queryForObject(
                "SELECT total FROM orcamento WHERE ordem_servico_id = ?", BigDecimal.class, ordemId);
    }
}
