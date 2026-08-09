package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.Dinheiro;
import br.com.oficinamecanica.ordemservico.domain.ItemServico;
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
@Table(name = "item_servico")
class ItemServicoJpaEntity {

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

    @Column(name = "servico_id", nullable = false)
    private UUID servicoId;

    @Column(name = "valor_mao_de_obra_snapshot", nullable = false, precision = PRECISAO, scale = ESCALA)
    private BigDecimal valorMaoDeObraSnapshot;

    @Column(name = "moeda", nullable = false, length = TAMANHO_DA_MOEDA)
    private String moeda;

    protected ItemServicoJpaEntity() {
    }

    static ItemServicoJpaEntity de(ItemServico item, OrdemServicoJpaEntity ordemServico,
                                   OrcamentoJpaEntity orcamentoOrigem) {
        ItemServicoJpaEntity entidade = new ItemServicoJpaEntity();
        entidade.id = item.id();
        entidade.ordemServico = ordemServico;
        entidade.orcamentoOrigem = orcamentoOrigem;
        entidade.servicoId = item.servicoId();
        entidade.valorMaoDeObraSnapshot = item.valorMaoDeObraSnapshot().valor();
        entidade.moeda = item.valorMaoDeObraSnapshot().moeda();
        return entidade;
    }

    UUID id() {
        return id;
    }

    ItemServico paraDominio() {
        return new ItemServico(id, servicoId, new Dinheiro(valorMaoDeObraSnapshot, moeda), orcamentoOrigem.versao());
    }
}
