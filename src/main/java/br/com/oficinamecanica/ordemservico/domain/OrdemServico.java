package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrdemServico {

    private static final String CODIGO = "ORDEM_SERVICO_INVALIDA";
    private static final int TAMANHO_MAXIMO_DO_RELATO = 1000;
    private static final int PRIMEIRA_VERSAO = 1;
    private static final StatusOrdemServico STATUS_INICIAL = StatusOrdemServico.RECEBIDA;

    private final UUID id;
    private final UUID clienteId;
    private final UUID veiculoId;
    private final CodigoAcompanhamento codigoAcompanhamento;
    private final String relatoDoProblema;
    private final LocalDateTime criadaEm;
    private final List<TransicaoStatus> transicoes;
    private final List<Orcamento> orcamentos;
    private final List<ItemServico> itensServico;
    private final List<ItemPeca> itensPeca;
    private StatusOrdemServico status;

    public OrdemServico(UUID id, UUID clienteId, UUID veiculoId, StatusOrdemServico status,
                        CodigoAcompanhamento codigoAcompanhamento, String relatoDoProblema,
                        LocalDateTime criadaEm, List<TransicaoStatus> transicoes,
                        List<Orcamento> orcamentos, List<ItemServico> itensServico, List<ItemPeca> itensPeca) {
        if (id == null) {
            throw new DadosInvalidosException(CODIGO, "Ordem de Servico exige identidade");
        }
        if (clienteId == null || veiculoId == null) {
            throw new DadosInvalidosException(CODIGO, "Ordem de Servico exige Cliente e Veiculo");
        }
        if (status == null) {
            throw new DadosInvalidosException(CODIGO, "Ordem de Servico exige Status da OS");
        }
        if (codigoAcompanhamento == null) {
            throw new DadosInvalidosException(CODIGO, "Ordem de Servico exige Codigo de acompanhamento");
        }
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.status = status;
        this.codigoAcompanhamento = codigoAcompanhamento;
        this.relatoDoProblema = relatoValidado(relatoDoProblema);
        this.criadaEm = criadaEm;
        this.transicoes = new ArrayList<>(transicoes);
        this.orcamentos = new ArrayList<>(orcamentos);
        this.itensServico = new ArrayList<>(itensServico);
        this.itensPeca = new ArrayList<>(itensPeca);
    }

    public static OrdemServico abrir(UUID clienteId, UUID veiculoId, String relatoDoProblema) {
        return new OrdemServico(UUID.randomUUID(), clienteId, veiculoId, STATUS_INICIAL,
                CodigoAcompanhamento.gerar(), relatoDoProblema, LocalDateTime.now(),
                List.of(TransicaoStatus.abertura(STATUS_INICIAL)), List.of(), List.of(), List.of());
    }

    public void iniciarDiagnostico() {
        transicionarPara(StatusOrdemServico.EM_DIAGNOSTICO);
    }

    public void incluirItens(List<ServicoAIncluir> servicos, List<PecaAIncluir> pecas) {
        exigirStatus(StatusOrdemServico.EM_DIAGNOSTICO);
        acrescentarItens(versaoCorrenteOuRascunho(), servicos, pecas);
    }

    public Orcamento concluirDiagnostico() {
        exigirStatus(StatusOrdemServico.EM_DIAGNOSTICO);
        Orcamento versao = versaoMaisRecente().orElseThrow(() -> new OrdemServicoSemOrcamentoException());
        versao.enviar();
        transicionarPara(StatusOrdemServico.AGUARDANDO_APROVACAO);
        return versao;
    }

    public Orcamento aprovarOrcamento() {
        exigirStatus(StatusOrdemServico.AGUARDANDO_APROVACAO);
        Orcamento versao = versaoAguardandoResposta();
        versao.aprovar();
        transicionarPara(StatusOrdemServico.EM_EXECUCAO);
        return versao;
    }

    public Orcamento registrarReparoAdicional(String descricao, List<ServicoAIncluir> servicos,
                                              List<PecaAIncluir> pecas) {
        exigirStatus(StatusOrdemServico.EM_EXECUCAO);
        Orcamento novaVersao = abrirRascunho(descricao);
        acrescentarItens(novaVersao, servicos, pecas);
        novaVersao.enviar();
        transicionarPara(StatusOrdemServico.AGUARDANDO_APROVACAO);
        return novaVersao;
    }

    public void concluirExecucao() {
        transicionarPara(StatusOrdemServico.FINALIZADA);
    }

    public void registrarEntrega() {
        transicionarPara(StatusOrdemServico.ENTREGUE);
    }

    public void reprovarOrcamento() {
        exigirStatus(StatusOrdemServico.AGUARDANDO_APROVACAO);
        StatusOrdemServico destino = destinoDaReprovacao();
        versaoAguardandoResposta().reprovar();
        transicionarPara(destino);
    }

    private StatusOrdemServico destinoDaReprovacao() {
        if (temVersaoAprovada()) {
            return StatusOrdemServico.EM_EXECUCAO;
        }
        return StatusOrdemServico.CANCELADA;
    }

    private Orcamento versaoAguardandoResposta() {
        return versaoMaisRecenteEnviada().orElseThrow(() -> new OrdemServicoSemOrcamentoException());
    }

    public List<ItemPeca> itensDePecaIntroduzidosPor(int versao) {
        return itensPeca.stream().filter(item -> item.versaoOrigem() == versao).toList();
    }

    public Optional<Duration> tempoEmExecucao() {
        if (!alcancou(StatusOrdemServico.FINALIZADA)) {
            return Optional.empty();
        }
        return Optional.of(somaDosSegmentosEmExecucao());
    }

    private boolean alcancou(StatusOrdemServico status) {
        return transicoes.stream().anyMatch(transicao -> transicao.paraStatus() == status);
    }

    private Duration somaDosSegmentosEmExecucao() {
        Duration total = Duration.ZERO;
        LocalDateTime inicioDoSegmento = null;
        for (TransicaoStatus transicao : transicoes) {
            if (inicioDoSegmento == null && transicao.paraStatus() == StatusOrdemServico.EM_EXECUCAO) {
                inicioDoSegmento = transicao.dataHora();
                continue;
            }
            if (inicioDoSegmento != null && transicao.deStatus() == StatusOrdemServico.EM_EXECUCAO) {
                total = total.plus(duracaoNaoNegativa(inicioDoSegmento, transicao.dataHora()));
                inicioDoSegmento = null;
            }
        }
        return total;
    }

    private Duration duracaoNaoNegativa(LocalDateTime inicio, LocalDateTime fim) {
        Duration duracao = Duration.between(inicio, fim);
        if (duracao.isNegative()) {
            return Duration.ZERO;
        }
        return duracao;
    }

    public Optional<Orcamento> versaoMaisRecente() {
        return orcamentos.stream().max(Comparator.comparingInt(Orcamento::versao));
    }

    public Optional<Orcamento> versaoMaisRecenteEnviada() {
        return orcamentos.stream().filter(Orcamento::enviado).max(Comparator.comparingInt(Orcamento::versao));
    }

    public boolean temVersaoAprovada() {
        return orcamentos.stream().anyMatch(Orcamento::aprovado);
    }

    public List<ItemServico> itensDeServicoDaVersao(int versao) {
        return itensServico.stream().filter(item -> cobertoPelaVersao(item.versaoOrigem(), versao)).toList();
    }

    public List<ItemPeca> itensDePecaDaVersao(int versao) {
        return itensPeca.stream().filter(item -> cobertoPelaVersao(item.versaoOrigem(), versao)).toList();
    }

    private void acrescentarItens(Orcamento versao, List<ServicoAIncluir> servicos, List<PecaAIncluir> pecas) {
        servicos.forEach(servico -> itensServico.add(
                ItemServico.incluir(servico.servicoId(), servico.valorMaoDeObra(), versao.versao())));
        pecas.forEach(peca -> itensPeca.add(
                ItemPeca.incluir(peca.pecaId(), peca.quantidade(), peca.preco(), versao.versao())));
        versao.consolidar(totalDaVersao(versao.versao()));
    }

    private Orcamento versaoCorrenteOuRascunho() {
        return versaoMaisRecente()
                .filter(orcamento -> !orcamento.enviado())
                .orElseGet(() -> abrirRascunho(null));
    }

    private Orcamento abrirRascunho(String descricao) {
        Orcamento rascunho = Orcamento.rascunho(proximaVersao(), descricao);
        orcamentos.add(rascunho);
        return rascunho;
    }

    private int proximaVersao() {
        return versaoMaisRecente().map(Orcamento::versao).map(versao -> versao + 1).orElse(PRIMEIRA_VERSAO);
    }

    private Dinheiro totalDaVersao(int versao) {
        Dinheiro totalDosServicos = itensDeServicoDaVersao(versao).stream()
                .map(ItemServico::subtotal)
                .reduce(Dinheiro.zero(), Dinheiro::somar);
        return itensDePecaDaVersao(versao).stream()
                .map(ItemPeca::subtotal)
                .reduce(totalDosServicos, Dinheiro::somar);
    }

    private boolean cobertoPelaVersao(int versaoOrigemDoItem, int versao) {
        if (versaoOrigemDoItem > versao) {
            return false;
        }
        if (versaoOrigemDoItem == versao) {
            return true;
        }
        return !versaoFoiReprovada(versaoOrigemDoItem);
    }

    private boolean versaoFoiReprovada(int versao) {
        return orcamentos.stream().filter(orcamento -> orcamento.versao() == versao).anyMatch(Orcamento::reprovado);
    }

    private void exigirStatus(StatusOrdemServico esperado) {
        if (status != esperado) {
            throw new EstadoExigidoException(status, esperado);
        }
    }

    private void transicionarPara(StatusOrdemServico destino) {
        if (!status.aceitaTransicaoPara(destino)) {
            throw new TransicaoInvalidaException(status, destino);
        }
        transicoes.add(TransicaoStatus.entre(status, destino));
        this.status = destino;
    }

    private String relatoValidado(String relato) {
        if (relato == null || relato.isBlank()) {
            return null;
        }
        String limpo = relato.trim();
        if (limpo.length() > TAMANHO_MAXIMO_DO_RELATO) {
            throw new DadosInvalidosException(CODIGO,
                    "Relato do problema excede " + TAMANHO_MAXIMO_DO_RELATO + " caracteres");
        }
        return limpo;
    }

    public UUID id() {
        return id;
    }

    public UUID clienteId() {
        return clienteId;
    }

    public UUID veiculoId() {
        return veiculoId;
    }

    public StatusOrdemServico status() {
        return status;
    }

    public CodigoAcompanhamento codigoAcompanhamento() {
        return codigoAcompanhamento;
    }

    public String relatoDoProblema() {
        return relatoDoProblema;
    }

    public LocalDateTime criadaEm() {
        return criadaEm;
    }

    public List<TransicaoStatus> transicoes() {
        return List.copyOf(transicoes);
    }

    public List<Orcamento> orcamentos() {
        return List.copyOf(orcamentos);
    }

    public List<ItemServico> itensServico() {
        return List.copyOf(itensServico);
    }

    public List<ItemPeca> itensPeca() {
        return List.copyOf(itensPeca);
    }
}
