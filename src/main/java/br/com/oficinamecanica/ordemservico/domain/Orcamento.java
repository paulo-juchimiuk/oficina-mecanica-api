package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.time.LocalDateTime;
import java.util.UUID;

public class Orcamento {

    private static final String CODIGO = "ORCAMENTO_INVALIDO";
    private static final int VALIDADE_PADRAO_EM_DIAS = 10;
    private static final int TAMANHO_MAXIMO_DA_DESCRICAO = 500;

    private final UUID id;
    private final int versao;
    private final int validadeDias;
    private final String descricao;
    private SituacaoOrcamento situacao;
    private Dinheiro total;
    private LocalDateTime dataEnvio;
    private LocalDateTime dataResposta;

    public Orcamento(UUID id, int versao, SituacaoOrcamento situacao, Dinheiro total, LocalDateTime dataEnvio,
                     LocalDateTime dataResposta, int validadeDias, String descricao) {
        if (id == null) {
            throw new DadosInvalidosException(CODIGO, "Versao do orcamento exige identidade");
        }
        if (versao < 1) {
            throw new DadosInvalidosException(CODIGO, "Versao do orcamento comeca em 1");
        }
        if (situacao == null) {
            throw new DadosInvalidosException(CODIGO, "Versao do orcamento exige situacao");
        }
        if (total == null) {
            throw new DadosInvalidosException(CODIGO, "Versao do orcamento exige total");
        }
        if (validadeDias < 1) {
            throw new DadosInvalidosException(CODIGO, "Versao do orcamento exige validade de pelo menos um dia");
        }
        this.id = id;
        this.versao = versao;
        this.situacao = situacao;
        this.total = total;
        this.dataEnvio = dataEnvio;
        this.dataResposta = dataResposta;
        this.validadeDias = validadeDias;
        this.descricao = descricaoValidada(descricao);
    }

    static Orcamento rascunho(int versao, String descricao) {
        return new Orcamento(UUID.randomUUID(), versao, SituacaoOrcamento.PENDENTE, Dinheiro.zero(),
                null, null, VALIDADE_PADRAO_EM_DIAS, descricao);
    }

    void consolidar(Dinheiro total) {
        if (enviado()) {
            throw new OrcamentoJaEnviadoException(versao);
        }
        this.total = total;
    }

    void enviar() {
        if (enviado()) {
            throw new OrcamentoJaEnviadoException(versao);
        }
        this.dataEnvio = LocalDateTime.now();
    }

    void aprovar() {
        registrarResposta(SituacaoOrcamento.APROVADO);
    }

    void reprovar() {
        registrarResposta(SituacaoOrcamento.REPROVADO);
    }

    private void registrarResposta(SituacaoOrcamento resposta) {
        if (!enviado()) {
            throw new OrcamentoNaoEnviadoException(versao);
        }
        if (respondido()) {
            throw new OrcamentoJaRespondidoException(versao, situacao);
        }
        this.situacao = resposta;
        this.dataResposta = LocalDateTime.now();
    }

    private String descricaoValidada(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = texto.trim();
        if (limpo.length() > TAMANHO_MAXIMO_DA_DESCRICAO) {
            throw new DadosInvalidosException(CODIGO,
                    "Descricao da Versao do orcamento excede " + TAMANHO_MAXIMO_DA_DESCRICAO + " caracteres");
        }
        return limpo;
    }

    public boolean enviado() {
        return dataEnvio != null;
    }

    public boolean respondido() {
        return situacao != SituacaoOrcamento.PENDENTE;
    }

    public boolean aprovado() {
        return situacao == SituacaoOrcamento.APROVADO;
    }

    public boolean reprovado() {
        return situacao == SituacaoOrcamento.REPROVADO;
    }

    public UUID id() {
        return id;
    }

    public int versao() {
        return versao;
    }

    public SituacaoOrcamento situacao() {
        return situacao;
    }

    public Dinheiro total() {
        return total;
    }

    public LocalDateTime dataEnvio() {
        return dataEnvio;
    }

    public LocalDateTime dataResposta() {
        return dataResposta;
    }

    public int validadeDias() {
        return validadeDias;
    }

    public String descricao() {
        return descricao;
    }
}
