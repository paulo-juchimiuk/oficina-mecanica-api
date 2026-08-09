package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.Dinheiro;
import br.com.oficinamecanica.ordemservico.domain.ItemPeca;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "item_peca")
class ItemPecaJpaEntity {

    private static final int TAMANHO_DA_MOEDA = 3;
    private static final int PRECISAO = 12;
    private static final int ESCALA = 2;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServicoJpaEntity ordemServico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_origem_id", nullable = false)
    private OrcamentoJpaEntity orcamentoOrigem;

    @Column(name = "peca_id", nullable = false)
    private UUID pecaId;

    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    @Column(name = "preco_snapshot", nullable = false, precision = PRECISAO, scale = ESCALA)
    private BigDecimal precoSnapshot;

    @Column(name = "moeda", nullable = false, length = TAMANHO_DA_MOEDA)
    private String moeda;

    protected ItemPecaJpaEntity() {
    }

    static ItemPecaJpaEntity de(ItemPeca item, OrdemServicoJpaEntity ordemServico,
                                OrcamentoJpaEntity orcamentoOrigem) {
        ItemPecaJpaEntity entidade = new ItemPecaJpaEntity();
        entidade.id = item.id();
        entidade.ordemServico = ordemServico;
        entidade.orcamentoOrigem = orcamentoOrigem;
        entidade.pecaId = item.pecaId();
        entidade.quantidade = item.quantidade();
        entidade.precoSnapshot = item.precoSnapshot().valor();
        entidade.moeda = item.precoSnapshot().moeda();
        return entidade;
    }

    UUID id() {
        return id;
    }

    ItemPeca paraDominio() {
        return new ItemPeca(id, pecaId, quantidade, new Dinheiro(precoSnapshot, moeda), orcamentoOrigem.versao());
    }
}
