package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Versao do orcamento")
class OrcamentoTest {

    private static final int PRIMEIRA_VERSAO = 1;
    private static final int VALIDADE_PADRAO = 10;
    private static final int TAMANHO_MAXIMO_DA_DESCRICAO = 500;

    private Dinheiro reais(String valor) {
        return new Dinheiro(new BigDecimal(valor), "BRL");
    }

    private Orcamento enviado() {
        Orcamento versao = Orcamento.rascunho(PRIMEIRA_VERSAO, null);
        versao.consolidar(reais("200.00"));
        versao.enviar();
        return versao;
    }

    @Test
    @DisplayName("deve nascer como rascunho pendente, zerado, sem data de envio e com validade de dez dias")
    void deveNascerComoRascunho() {
        Orcamento rascunho = Orcamento.rascunho(PRIMEIRA_VERSAO, null);

        assertThat(rascunho.versao()).isEqualTo(PRIMEIRA_VERSAO);
        assertThat(rascunho.situacao()).isEqualTo(SituacaoOrcamento.PENDENTE);
        assertThat(rascunho.total()).isEqualTo(Dinheiro.zero());
        assertThat(rascunho.dataEnvio()).isNull();
        assertThat(rascunho.validadeDias()).isEqualTo(VALIDADE_PADRAO);
        assertThat(rascunho.enviado()).isFalse();
        assertThat(rascunho.respondido()).isFalse();
    }

    @Test
    @DisplayName("deve aceitar consolidar o total enquanto o rascunho nao foi enviado")
    void deveConsolidarAntesDoEnvio() {
        Orcamento rascunho = Orcamento.rascunho(PRIMEIRA_VERSAO, null);

        rascunho.consolidar(reais("150.00"));
        rascunho.consolidar(reais("340.50"));

        assertThat(rascunho.total()).isEqualTo(reais("340.50"));
    }

    @Test
    @DisplayName("deve ficar imutavel depois de enviada, que e o que o ADR-006 exige")
    void deveFicarImutavelDepoisDeEnviada() {
        Orcamento versao = enviado();

        assertThatThrownBy(() -> versao.consolidar(reais("999.00")))
                .isInstanceOf(OrcamentoJaEnviadoException.class);
        assertThat(versao.total()).isEqualTo(reais("200.00"));
    }

    @Test
    @DisplayName("deve recusar enviar duas vezes a mesma versao")
    void deveRecusarEnvioRepetido() {
        Orcamento versao = enviado();

        assertThatThrownBy(versao::enviar).isInstanceOf(OrcamentoJaEnviadoException.class);
    }

    @Test
    @DisplayName("deve registrar data de envio ao enviar")
    void deveRegistrarDataDeEnvio() {
        Orcamento versao = enviado();

        assertThat(versao.enviado()).isTrue();
        assertThat(versao.dataEnvio()).isNotNull();
    }

    @Test
    @DisplayName("deve recusar resposta do Cliente antes de a versao ter sido enviada")
    void deveRecusarRespostaAntesDoEnvio() {
        Orcamento rascunho = Orcamento.rascunho(PRIMEIRA_VERSAO, null);

        assertThatThrownBy(rascunho::aprovar).isInstanceOf(OrcamentoNaoEnviadoException.class);
        assertThatThrownBy(rascunho::reprovar).isInstanceOf(OrcamentoNaoEnviadoException.class);
    }

    @Test
    @DisplayName("deve aprovar a versao enviada e registrar a data de resposta")
    void deveAprovar() {
        Orcamento versao = enviado();

        versao.aprovar();

        assertThat(versao.situacao()).isEqualTo(SituacaoOrcamento.APROVADO);
        assertThat(versao.aprovado()).isTrue();
        assertThat(versao.respondido()).isTrue();
        assertThat(versao.dataResposta()).isNotNull();
    }

    @Test
    @DisplayName("deve reprovar a versao enviada e registrar a data de resposta")
    void deveReprovar() {
        Orcamento versao = enviado();

        versao.reprovar();

        assertThat(versao.situacao()).isEqualTo(SituacaoOrcamento.REPROVADO);
        assertThat(versao.reprovado()).isTrue();
        assertThat(versao.dataResposta()).isNotNull();
    }

    @Test
    @DisplayName("deve recusar responder duas vezes a mesma versao")
    void deveRecusarRespostaRepetida() {
        Orcamento aprovada = enviado();
        aprovada.aprovar();
        Orcamento reprovada = enviado();
        reprovada.reprovar();

        assertThatThrownBy(aprovada::reprovar).isInstanceOf(OrcamentoJaRespondidoException.class);
        assertThatThrownBy(reprovada::aprovar).isInstanceOf(OrcamentoJaRespondidoException.class);
    }

    @Test
    @DisplayName("deve guardar a descricao do Reparo adicional, ignorando texto em branco")
    void deveGuardarADescricao() {
        assertThat(Orcamento.rascunho(2, "  Troca da bomba d agua  ").descricao())
                .isEqualTo("Troca da bomba d agua");
        assertThat(Orcamento.rascunho(2, "   ").descricao()).isNull();
        assertThat(Orcamento.rascunho(2, null).descricao()).isNull();
    }

    @Test
    @DisplayName("deve recusar descricao maior que o limite do contrato")
    void deveRecusarDescricaoAcimaDoLimite() {
        String longaDemais = "a".repeat(TAMANHO_MAXIMO_DA_DESCRICAO + 1);

        assertThatThrownBy(() -> Orcamento.rascunho(2, longaDemais))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    @DisplayName("deve exigir identidade, versao a partir de 1, situacao, total e validade ao reidratar do banco")
    void deveExigirCamposObrigatoriosAoReidratar() {
        UUID id = UUID.randomUUID();
        Dinheiro total = reais("10.00");

        assertThatThrownBy(() -> new Orcamento(null, 1, SituacaoOrcamento.PENDENTE, total, null, null, 10, null))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new Orcamento(id, 0, SituacaoOrcamento.PENDENTE, total, null, null, 10, null))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new Orcamento(id, 1, null, total, null, null, 10, null))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new Orcamento(id, 1, SituacaoOrcamento.PENDENTE, null, null, null, 10, null))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new Orcamento(id, 1, SituacaoOrcamento.PENDENTE, total, null, null, 0, null))
                .isInstanceOf(DadosInvalidosException.class);
    }
}
