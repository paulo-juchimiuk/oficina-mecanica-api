package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.Dinheiro;
import br.com.oficinamecanica.ordemservico.domain.Orcamento;
import br.com.oficinamecanica.ordemservico.domain.SituacaoOrcamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orcamento")
class OrcamentoJpaEntity {

    private static final int TAMANHO_DA_SITUACAO = 12;
    private static final int TAMANHO_DA_MOEDA = 3;
    private static final int TAMANHO_DA_DESCRICAO = 500;
    private static final int PRECISAO = 12;
    private static final int ESCALA = 2;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServicoJpaEntity ordemServico;

    @Column(name = "versao", nullable = false)
    private int versao;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = TAMANHO_DA_SITUACAO)
    private SituacaoOrcamento situacao;

    @Column(name = "total", nullable = false, precision = PRECISAO, scale = ESCALA)
    private BigDecimal total;

    @Column(name = "moeda", nullable = false, length = TAMANHO_DA_MOEDA)
    private String moeda;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;

    @Column(name = "validade_dias", nullable = false)
    private int validadeDias;

    @Column(name = "descricao", length = TAMANHO_DA_DESCRICAO)
    private String descricao;

    protected OrcamentoJpaEntity() {
    }

    static OrcamentoJpaEntity de(Orcamento orcamento, OrdemServicoJpaEntity ordemServico) {
        OrcamentoJpaEntity entidade = new OrcamentoJpaEntity();
        entidade.id = orcamento.id();
        entidade.ordemServico = ordemServico;
        entidade.versao = orcamento.versao();
        entidade.validadeDias = orcamento.validadeDias();
        entidade.descricao = orcamento.descricao();
        entidade.sincronizar(orcamento);
        return entidade;
    }

    void sincronizar(Orcamento orcamento) {
        this.situacao = orcamento.situacao();
        this.total = orcamento.total().valor();
        this.moeda = orcamento.total().moeda();
        this.dataEnvio = orcamento.dataEnvio();
        this.dataResposta = orcamento.dataResposta();
    }

    Orcamento paraDominio() {
        return new Orcamento(id, versao, situacao, new Dinheiro(total, moeda),
                dataEnvio, dataResposta, validadeDias, descricao);
    }

    UUID id() {
        return id;
    }

    int versao() {
        return versao;
    }
}
