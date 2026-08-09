package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import br.com.oficinamecanica.ordemservico.domain.TransicaoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transicao_status")
class TransicaoStatusJpaEntity {

    private static final int TAMANHO_DO_STATUS = 24;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServicoJpaEntity ordemServico;

    @Enumerated(EnumType.STRING)
    @Column(name = "de_status", length = TAMANHO_DO_STATUS)
    private StatusOrdemServico deStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "para_status", nullable = false, length = TAMANHO_DO_STATUS)
    private StatusOrdemServico paraStatus;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    protected TransicaoStatusJpaEntity() {
    }

    static TransicaoStatusJpaEntity de(TransicaoStatus transicao, OrdemServicoJpaEntity ordemServico) {
        TransicaoStatusJpaEntity entidade = new TransicaoStatusJpaEntity();
        entidade.id = UUID.randomUUID();
        entidade.ordemServico = ordemServico;
        entidade.deStatus = transicao.deStatus();
        entidade.paraStatus = transicao.paraStatus();
        entidade.dataHora = transicao.dataHora();
        return entidade;
    }

    TransicaoStatus paraDominio() {
        return new TransicaoStatus(deStatus, paraStatus, dataHora);
    }
}
