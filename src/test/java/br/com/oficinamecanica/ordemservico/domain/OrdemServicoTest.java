package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agregado Ordem de Servico")
class OrdemServicoTest {

    private static final String RELATO = "Barulho ao frear em baixa velocidade";
    private static final int TAMANHO_MAXIMO_DO_RELATO = 1000;

    private OrdemServico ordemAberta() {
        return OrdemServico.abrir(UUID.randomUUID(), UUID.randomUUID(), RELATO);
    }

    @Test
    @DisplayName("deve nascer Recebida, com Codigo de acompanhamento e a transicao de abertura registrada")
    void deveNascerRecebida() {
        OrdemServico ordem = ordemAberta();

        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.RECEBIDA);
        assertThat(ordem.codigoAcompanhamento()).isNotNull();
        assertThat(ordem.transicoes()).singleElement().satisfies(transicao -> {
            assertThat(transicao.deStatus()).isNull();
            assertThat(transicao.paraStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
            assertThat(transicao.dataHora()).isNotNull();
        });
    }

    @Test
    @DisplayName("deve exigir Cliente e Veiculo na abertura")
    void deveExigirClienteEVeiculo() {
        assertThatThrownBy(() -> OrdemServico.abrir(null, UUID.randomUUID(), RELATO))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> OrdemServico.abrir(UUID.randomUUID(), null, RELATO))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve aceitar Ordem de Servico sem Relato do problema")
    void deveAceitarOrdemSemRelato() {
        assertThat(OrdemServico.abrir(UUID.randomUUID(), UUID.randomUUID(), "  ").relatoDoProblema()).isNull();
        assertThat(OrdemServico.abrir(UUID.randomUUID(), UUID.randomUUID(), null).relatoDoProblema()).isNull();
    }

    @Test
    @DisplayName("deve recusar Relato do problema maior que o limite do contrato")
    void deveRecusarRelatoAcimaDoLimite() {
        String longoDemais = "a".repeat(TAMANHO_MAXIMO_DO_RELATO + 1);
        assertThatThrownBy(() -> OrdemServico.abrir(UUID.randomUUID(), UUID.randomUUID(), longoDemais))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve levar a OS de Recebida a Em diagnostico e registrar a transicao")
    void deveIniciarDiagnostico() {
        OrdemServico ordem = ordemAberta();

        ordem.iniciarDiagnostico();

        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.EM_DIAGNOSTICO);
        assertThat(ordem.transicoes()).hasSize(2);
        assertThat(ordem.transicoes().getLast()).satisfies(transicao -> {
            assertThat(transicao.deStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
            assertThat(transicao.paraStatus()).isEqualTo(StatusOrdemServico.EM_DIAGNOSTICO);
        });
    }

    @Test
    @DisplayName("deve recusar iniciar o diagnostico de uma OS que ja saiu de Recebida")
    void deveRecusarIniciarDiagnosticoForaDeRecebida() {
        OrdemServico ordem = ordemAberta();
        ordem.iniciarDiagnostico();

        assertThatThrownBy(ordem::iniciarDiagnostico)
                .isInstanceOf(TransicaoInvalidaException.class)
                .hasMessageContaining(StatusOrdemServico.EM_DIAGNOSTICO.name());
    }

    @Test
    @DisplayName("deve exigir identidade, Status da OS e Codigo de acompanhamento ao reidratar do banco")
    void deveExigirCamposObrigatoriosAoReidratar() {
        UUID id = UUID.randomUUID();
        CodigoAcompanhamento codigo = CodigoAcompanhamento.gerar();
        LocalDateTime agora = LocalDateTime.now();
        List<TransicaoStatus> historico = List.of(TransicaoStatus.abertura(StatusOrdemServico.RECEBIDA));

        assertThatThrownBy(() -> new OrdemServico(null, id, id, StatusOrdemServico.RECEBIDA,
                codigo, RELATO, agora, historico, List.of(), List.of(), List.of()))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new OrdemServico(id, id, id, null, codigo, RELATO, agora, historico, List.of(), List.of(), List.of()))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new OrdemServico(id, id, id, StatusOrdemServico.RECEBIDA,
                null, RELATO, agora, historico, List.of(), List.of(), List.of()))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("nao deve permitir alterar o historico de transicoes por fora do agregado")
    void naoDevePermitirAlterarOHistoricoPorFora() {
        OrdemServico ordem = ordemAberta();

        assertThatThrownBy(() -> ordem.transicoes().add(TransicaoStatus.abertura(StatusOrdemServico.ENTREGUE)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(ordem.transicoes()).hasSize(1);
    }

    private Dinheiro reais(String valor) {
        return new Dinheiro(new BigDecimal(valor), "BRL");
    }

    private OrdemServico ordemEmDiagnostico() {
        OrdemServico ordem = ordemAberta();
        ordem.iniciarDiagnostico();
        return ordem;
    }

    @Test
    @DisplayName("deve abrir a versao 1 como rascunho na inclusao do primeiro item, como o ADR-006 descreve")
    void deveAbrirAVersaoUmComoRascunhoNoPrimeiroItem() {
        OrdemServico ordem = ordemEmDiagnostico();

        ordem.incluirItens(List.of(new ServicoAIncluir(UUID.randomUUID(), reais("120.00"))), List.of());

        assertThat(ordem.orcamentos()).singleElement().satisfies(versao -> {
            assertThat(versao.versao()).isEqualTo(1);
            assertThat(versao.situacao()).isEqualTo(SituacaoOrcamento.PENDENTE);
            assertThat(versao.enviado()).isFalse();
            assertThat(versao.dataEnvio()).isNull();
            assertThat(versao.total()).isEqualTo(reais("120.00"));
        });
    }

    @Test
    @DisplayName("deve somar servicos e pecas no total da versao, multiplicando a peca pela quantidade")
    void deveSomarOTotalDaVersao() {
        OrdemServico ordem = ordemEmDiagnostico();

        ordem.incluirItens(
                List.of(new ServicoAIncluir(UUID.randomUUID(), reais("120.00"))),
                List.of(new PecaAIncluir(UUID.randomUUID(), 3, reais("50.00"))));

        assertThat(ordem.versaoMaisRecente().orElseThrow().total()).isEqualTo(reais("270.00"));
    }

    @Test
    @DisplayName("deve acumular novas inclusoes na MESMA versao enquanto ela nao foi enviada")
    void deveAcumularNaMesmaVersaoAntesDoEnvio() {
        OrdemServico ordem = ordemEmDiagnostico();

        ordem.incluirItens(List.of(new ServicoAIncluir(UUID.randomUUID(), reais("120.00"))), List.of());
        ordem.incluirItens(List.of(new ServicoAIncluir(UUID.randomUUID(), reais("80.00"))), List.of());

        assertThat(ordem.orcamentos()).hasSize(1);
        assertThat(ordem.itensServico()).hasSize(2).allSatisfy(item ->
                assertThat(item.versaoOrigem()).isEqualTo(1));
        assertThat(ordem.versaoMaisRecente().orElseThrow().total()).isEqualTo(reais("200.00"));
    }

    @Test
    @DisplayName("deve recusar incluir itens fora de Em diagnostico")
    void deveRecusarIncluirItensForaDeEmDiagnostico() {
        OrdemServico ordem = ordemAberta();

        assertThatThrownBy(() -> ordem.incluirItens(
                List.of(new ServicoAIncluir(UUID.randomUUID(), reais("120.00"))), List.of()))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    @DisplayName("deve enviar a versao e levar a OS a Aguardando aprovacao ao concluir o diagnostico")
    void deveEnviarAVersaoAoConcluirODiagnostico() {
        OrdemServico ordem = ordemEmDiagnostico();
        ordem.incluirItens(List.of(new ServicoAIncluir(UUID.randomUUID(), reais("120.00"))), List.of());

        ordem.concluirDiagnostico();

        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.AGUARDANDO_APROVACAO);
        assertThat(ordem.versaoMaisRecenteEnviada()).isPresent();
        assertThat(ordem.versaoMaisRecente().orElseThrow().dataEnvio()).isNotNull();
        assertThat(ordem.transicoes()).hasSize(3);
    }

    @Test
    @DisplayName("deve recusar concluir o diagnostico sem nenhum item, porque nao ha Orcamento a enviar")
    void deveRecusarConcluirDiagnosticoSemItens() {
        OrdemServico ordem = ordemEmDiagnostico();

        assertThatThrownBy(ordem::concluirDiagnostico)
                .isInstanceOf(OrdemServicoSemOrcamentoException.class);
        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.EM_DIAGNOSTICO);
    }

    @Test
    @DisplayName("deve recusar concluir o diagnostico de uma OS que nao esta Em diagnostico")
    void deveRecusarConcluirDiagnosticoForaDeEmDiagnostico() {
        OrdemServico ordem = ordemAberta();

        assertThatThrownBy(ordem::concluirDiagnostico).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    @DisplayName("deve recusar incluir itens depois de a versao ter sido enviada, porque ela e imutavel")
    void deveRecusarIncluirItensDepoisDoEnvio() {
        OrdemServico ordem = ordemEmDiagnostico();
        ordem.incluirItens(List.of(new ServicoAIncluir(UUID.randomUUID(), reais("120.00"))), List.of());
        ordem.concluirDiagnostico();

        assertThatThrownBy(() -> ordem.incluirItens(
                List.of(new ServicoAIncluir(UUID.randomUUID(), reais("10.00"))), List.of()))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    @DisplayName("deve trazer no escopo da versao 1 os itens que ela introduziu")
    void deveTrazerOEscopoDaVersaoUm() {
        OrdemServico ordem = ordemEmDiagnostico();
        ordem.incluirItens(
                List.of(new ServicoAIncluir(UUID.randomUUID(), reais("120.00"))),
                List.of(new PecaAIncluir(UUID.randomUUID(), 2, reais("50.00"))));

        assertThat(ordem.itensDeServicoDaVersao(1)).hasSize(1);
        assertThat(ordem.itensDePecaDaVersao(1)).hasSize(1);
    }

    @Test
    @DisplayName("deve nascer sem versao aprovada, que e o que o guard da reprovacao pergunta")
    void deveNascerSemVersaoAprovada() {
        assertThat(ordemAberta().temVersaoAprovada()).isFalse();
    }

    private OrdemServico ordemAguardandoAprovacao() {
        OrdemServico ordem = ordemEmDiagnostico();
        ordem.incluirItens(
                List.of(new ServicoAIncluir(UUID.randomUUID(), reais("120.00"))),
                List.of(new PecaAIncluir(UUID.randomUUID(), 2, reais("50.00"))));
        ordem.concluirDiagnostico();
        return ordem;
    }

    @Test
    @DisplayName("deve aprovar o orcamento e levar a OS a Em execucao, por transicao automatica")
    void deveAprovarELevarAEmExecucao() {
        OrdemServico ordem = ordemAguardandoAprovacao();

        ordem.aprovarOrcamento();

        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.EM_EXECUCAO);
        assertThat(ordem.temVersaoAprovada()).isTrue();
        assertThat(ordem.versaoMaisRecente().orElseThrow().situacao())
                .isEqualTo(SituacaoOrcamento.APROVADO);
        assertThat(ordem.versaoMaisRecente().orElseThrow().dataResposta()).isNotNull();
    }

    @Test
    @DisplayName("deve levar a OS a Cancelada quando o Cliente reprova e NAO existe versao aprovada")
    void deveCancelarNaReprovacaoSemVersaoAprovada() {
        OrdemServico ordem = ordemAguardandoAprovacao();

        ordem.reprovarOrcamento();

        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.CANCELADA);
        assertThat(ordem.status().encerrado()).isTrue();
        assertThat(ordem.versaoMaisRecente().orElseThrow().situacao())
                .isEqualTo(SituacaoOrcamento.REPROVADO);
    }

    @Test
    @DisplayName("deve devolver a OS a Em execucao quando o Cliente reprova e JA existe versao aprovada")
    void deveVoltarAEmExecucaoNaReprovacaoComVersaoAprovada() {
        OrdemServico ordem = ordemComVersaoAprovadaEOutraPendente();

        ordem.reprovarOrcamento();

        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.EM_EXECUCAO);
        assertThat(ordem.temVersaoAprovada()).isTrue();
    }

    @Test
    @DisplayName("deve recusar aprovar ou reprovar fora de Aguardando aprovacao, porque o poder expira pela maquina")
    void deveRecusarRespostaForaDeAguardandoAprovacao() {
        OrdemServico emDiagnostico = ordemEmDiagnostico();

        assertThatThrownBy(emDiagnostico::aprovarOrcamento).isInstanceOf(TransicaoInvalidaException.class);
        assertThatThrownBy(emDiagnostico::reprovarOrcamento).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    @DisplayName("deve recusar responder duas vezes, porque a OS ja saiu de Aguardando aprovacao")
    void deveRecusarSegundaResposta() {
        OrdemServico ordem = ordemAguardandoAprovacao();
        ordem.aprovarOrcamento();

        assertThatThrownBy(ordem::aprovarOrcamento).isInstanceOf(TransicaoInvalidaException.class);
        assertThatThrownBy(ordem::reprovarOrcamento).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    @DisplayName("deve reservar somente as pecas introduzidas pela versao aprovada")
    void deveIsolarAsPecasIntroduzidasPelaVersao() {
        OrdemServico ordem = ordemAguardandoAprovacao();

        assertThat(ordem.itensDePecaIntroduzidosPor(1)).hasSize(1);
        assertThat(ordem.itensDePecaIntroduzidosPor(2)).isEmpty();
    }

    private OrdemServico ordemEmExecucao() {
        OrdemServico ordem = ordemAguardandoAprovacao();
        ordem.aprovarOrcamento();
        return ordem;
    }

    @Test
    @DisplayName("deve gerar a versao 2 ja enviada e devolver a OS a Aguardando aprovacao no reparo adicional")
    void deveGerarNovaVersaoNoReparoAdicional() {
        OrdemServico ordem = ordemEmExecucao();

        ordem.registrarReparoAdicional("Troca da bomba d agua",
                List.of(new ServicoAIncluir(UUID.randomUUID(), reais("80.00"))), List.of());

        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.AGUARDANDO_APROVACAO);
        assertThat(ordem.orcamentos()).hasSize(2);
        assertThat(ordem.versaoMaisRecente().orElseThrow()).satisfies(versao -> {
            assertThat(versao.versao()).isEqualTo(2);
            assertThat(versao.descricao()).isEqualTo("Troca da bomba d agua");
            assertThat(versao.enviado()).isTrue();
            assertThat(versao.situacao()).isEqualTo(SituacaoOrcamento.PENDENTE);
        });
    }

    @Test
    @DisplayName("deve manter a versao anterior imutavel e aprovada depois do reparo adicional")
    void deveManterAVersaoAnteriorImutavel() {
        OrdemServico ordem = ordemEmExecucao();
        Dinheiro totalDaPrimeira = ordem.orcamentos().getFirst().total();

        ordem.registrarReparoAdicional("Troca da bomba d agua",
                List.of(new ServicoAIncluir(UUID.randomUUID(), reais("80.00"))), List.of());

        assertThat(ordem.orcamentos().getFirst().situacao()).isEqualTo(SituacaoOrcamento.APROVADO);
        assertThat(ordem.orcamentos().getFirst().total()).isEqualTo(totalDaPrimeira);
    }

    @Test
    @DisplayName("deve somar no total da versao 2 o escopo que ela cobre, incluindo os itens da versao 1")
    void deveSomarOEscopoCobertoPelaVersaoDois() {
        OrdemServico ordem = ordemEmExecucao();

        ordem.registrarReparoAdicional("Troca da bomba d agua",
                List.of(new ServicoAIncluir(UUID.randomUUID(), reais("80.00"))), List.of());

        assertThat(ordem.versaoMaisRecente().orElseThrow().total()).isEqualTo(reais("300.00"));
        assertThat(ordem.itensDeServicoDaVersao(2)).hasSize(2);
        assertThat(ordem.itensDeServicoDaVersao(1)).hasSize(1);
        assertThat(ordem.itensDePecaIntroduzidosPor(2)).isEmpty();
    }

    @Test
    @DisplayName("deve recusar reparo adicional fora de Em execucao, porque ele nasce durante a execucao")
    void deveRecusarReparoAdicionalForaDeEmExecucao() {
        OrdemServico aguardando = ordemAguardandoAprovacao();

        assertThatThrownBy(() -> aguardando.registrarReparoAdicional("Qualquer coisa",
                List.of(new ServicoAIncluir(UUID.randomUUID(), reais("80.00"))), List.of()))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    @DisplayName("deve devolver a OS a Em execucao quando o reparo adicional e reprovado, sem levar o escopo recusado")
    void deveVoltarAEmExecucaoQuandoOReparoAdicionalEReprovado() {
        OrdemServico ordem = ordemEmExecucao();
        ordem.registrarReparoAdicional("Troca da bomba d agua",
                List.of(new ServicoAIncluir(UUID.randomUUID(), reais("80.00"))), List.of());

        ordem.reprovarOrcamento();

        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.EM_EXECUCAO);
        assertThat(ordem.orcamentos().getFirst().situacao()).isEqualTo(SituacaoOrcamento.APROVADO);
        assertThat(ordem.versaoMaisRecente().orElseThrow().situacao()).isEqualTo(SituacaoOrcamento.REPROVADO);
        assertThat(ordem.itensDeServicoDaVersao(2)).hasSize(2);
    }

    @Test
    @DisplayName("deve concluir a execucao e depois registrar a entrega, fechando o ciclo")
    void deveConcluirExecucaoERegistrarEntrega() {
        OrdemServico ordem = ordemEmExecucao();

        ordem.concluirExecucao();
        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.FINALIZADA);

        ordem.registrarEntrega();
        assertThat(ordem.status()).isEqualTo(StatusOrdemServico.ENTREGUE);
        assertThat(ordem.status().encerrado()).isTrue();
    }

    @Test
    @DisplayName("deve recusar concluir a execucao e registrar a entrega fora de ordem")
    void deveRecusarConclusaoEEntregaForaDeOrdem() {
        OrdemServico aguardando = ordemAguardandoAprovacao();
        assertThatThrownBy(aguardando::concluirExecucao).isInstanceOf(TransicaoInvalidaException.class);

        OrdemServico emExecucao = ordemEmExecucao();
        assertThatThrownBy(emExecucao::registrarEntrega).isInstanceOf(TransicaoInvalidaException.class);

        emExecucao.concluirExecucao();
        emExecucao.registrarEntrega();
        assertThatThrownBy(emExecucao::registrarEntrega).isInstanceOf(TransicaoInvalidaException.class);
    }

    private OrdemServico ordemComVersaoAprovadaEOutraPendente() {
        UUID id = UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.now();
        Orcamento aprovada = new Orcamento(UUID.randomUUID(), 1, SituacaoOrcamento.APROVADO,
                reais("200.00"), agora, agora, 10, null);
        Orcamento pendente = new Orcamento(UUID.randomUUID(), 2, SituacaoOrcamento.PENDENTE,
                reais("80.00"), agora, null, 10, "Troca da bomba d agua");
        return new OrdemServico(id, UUID.randomUUID(), UUID.randomUUID(),
                StatusOrdemServico.AGUARDANDO_APROVACAO, CodigoAcompanhamento.gerar(), RELATO, agora,
                List.of(TransicaoStatus.abertura(StatusOrdemServico.RECEBIDA)),
                List.of(aprovada, pendente), List.of(), List.of());
    }

    @Test
    @DisplayName("nao deve permitir alterar orcamentos e itens por fora do agregado")
    void naoDevePermitirAlterarOrcamentosEItensPorFora() {
        OrdemServico ordem = ordemEmDiagnostico();
        ordem.incluirItens(
                List.of(new ServicoAIncluir(UUID.randomUUID(), reais("120.00"))),
                List.of(new PecaAIncluir(UUID.randomUUID(), 1, reais("50.00"))));

        assertThatThrownBy(() -> ordem.orcamentos().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ordem.itensServico().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ordem.itensPeca().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
