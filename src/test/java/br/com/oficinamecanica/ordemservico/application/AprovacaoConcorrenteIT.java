package br.com.oficinamecanica.ordemservico.application;

import br.com.oficinamecanica.shared.domain.ConflitoDeEstadoException;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.OrdemServicoRepository;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import br.com.oficinamecanica.suporte.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Concorrencia na resposta do Cliente ao Orcamento")
class AprovacaoConcorrenteIT extends IntegracaoBase {

    private static final int QUANTIDADE_DE_PECAS = 2;
    private static final int SALDO_INICIAL = 50;
    private static final int RESPOSTAS_SIMULTANEAS = 4;

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
    private ReprovarOrcamentoUseCase reprovarOrcamento;

    @Autowired
    private OrdemServicoRepository ordensServico;

    private UUID pecaId;
    private String codigo;

    @BeforeEach
    void prepararOrdemAguardandoAprovacao() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID servicoId = UUID.randomUUID();
        pecaId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, nome, documento, email) VALUES (?, ?, ?, ?)",
                clienteId, "Ana Beatriz Souza", "10433218100", "ana@example.com");
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
                VALUES (?, 'Filtro de oleo', 'unidade', 50.00, 'BRL', ?, 0, 2)
                """, pecaId, SALDO_INICIAL);

        OrdemServico ordem = criarOrdemServico.executar("10433218100", veiculoId, "Barulho ao frear", List.of(), List.of());
        iniciarDiagnostico.executar(ordem.id());
        incluirItens.executar(ordem.id(), List.of(new ItemDeServicoRequisitado(servicoId)),
                List.of(new ItemDePecaRequisitado(pecaId, QUANTIDADE_DE_PECAS)));
        concluirDiagnostico.executar(ordem.id());
        codigo = ordem.codigoAcompanhamento().valor();
    }

    @Test
    @DisplayName("deve aceitar uma unica aprovacao entre varias simultaneas, sem reservar peca a mais")
    void deveAceitarUmaUnicaAprovacao() throws Exception {
        int aceitas = disparar(RESPOSTAS_SIMULTANEAS, () -> aprovarOrcamento.executar(codigo));

        assertThat(aceitas).isEqualTo(1);
        assertThat(contar("SELECT COUNT(*) FROM reserva_peca WHERE peca_id = '" + pecaId + "'")).isEqualTo(1);
        assertThat(contar("SELECT quantidade_reservada FROM peca WHERE id = '" + pecaId + "'"))
                .isEqualTo(QUANTIDADE_DE_PECAS);
        assertThat(transicoesParaEmExecucao()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve aceitar uma unica resposta quando aprovacao e reprovacao correm juntas")
    void deveAceitarUmaUnicaRespostaEntreAprovarEReprovar() throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> corridas = List.of(
                executor.submit(tentativa(largada, () -> aprovarOrcamento.executar(codigo))),
                executor.submit(tentativa(largada, () -> reprovarOrcamento.executar(codigo))));

        largada.countDown();
        int aceitas = contarSucessos(corridas);
        encerrar(executor);

        assertThat(aceitas).isEqualTo(1);
        assertThat(situacaoFinal()).isIn(StatusOrdemServico.EM_EXECUCAO.name(),
                StatusOrdemServico.CANCELADA.name());
        assertThat(transicoesDaOrdem()).isEqualTo(4);
    }

    private int disparar(int quantidade, Callable<?> operacao) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(quantidade);
        List<Future<Boolean>> corridas = java.util.stream.IntStream.range(0, quantidade)
                .mapToObj(vez -> executor.submit(tentativa(largada, operacao)))
                .toList();

        largada.countDown();
        int aceitas = contarSucessos(corridas);
        encerrar(executor);
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

    private int contarSucessos(List<Future<Boolean>> corridas) throws Exception {
        int aceitas = 0;
        for (Future<Boolean> corrida : corridas) {
            aceitas += Boolean.TRUE.equals(corrida.get(60, TimeUnit.SECONDS)) ? 1 : 0;
        }
        return aceitas;
    }

    private void encerrar(ExecutorService executor) throws Exception {
        executor.shutdown();
        assertThat(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
    }

    private int transicoesParaEmExecucao() {
        return contar("""
                SELECT COUNT(*) FROM transicao_status transicao
                JOIN ordem_servico ordem ON ordem.id = transicao.ordem_servico_id
                WHERE ordem.codigo_acompanhamento = '%s' AND transicao.para_status = 'EM_EXECUCAO'
                """.formatted(codigo));
    }

    private int transicoesDaOrdem() {
        return contar("""
                SELECT COUNT(*) FROM transicao_status transicao
                JOIN ordem_servico ordem ON ordem.id = transicao.ordem_servico_id
                WHERE ordem.codigo_acompanhamento = '%s'
                """.formatted(codigo));
    }

    private String situacaoFinal() {
        return jdbc.queryForObject(
                "SELECT status FROM ordem_servico WHERE codigo_acompanhamento = ?", String.class, codigo);
    }

    private int contar(String consulta) {
        return jdbc.queryForObject(consulta, Integer.class);
    }
}
