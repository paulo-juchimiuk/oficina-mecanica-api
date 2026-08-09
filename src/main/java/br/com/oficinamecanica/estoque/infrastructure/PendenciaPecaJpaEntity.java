package br.com.oficinamecanica.estoque.infrastructure;

import br.com.oficinamecanica.estoque.domain.PendenciaPeca;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pendencia_peca")
class PendenciaPecaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "ordem_servico_id", nullable = false)
    private UUID ordemServicoId;

    @Column(name = "peca_id", nullable = false)
    private UUID pecaId;

    @Column(name = "quantidade_faltante", nullable = false)
    private int quantidadeFaltante;

    @Column(name = "detectada_em", nullable = false)
    private LocalDateTime detectadaEm;

    @Column(name = "resolvida_em")
    private LocalDateTime resolvidaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peca_id", insertable = false, updatable = false)
    private PecaJpaEntity peca;

    protected PendenciaPecaJpaEntity() {
    }

    static PendenciaPecaJpaEntity de(PendenciaPeca pendencia, PecaJpaEntity peca) {
        PendenciaPecaJpaEntity entidade = new PendenciaPecaJpaEntity();
        entidade.id = pendencia.id();
        entidade.ordemServicoId = pendencia.ordemServicoId();
        entidade.pecaId = pendencia.pecaId();
        entidade.quantidadeFaltante = pendencia.quantidadeFaltante();
        entidade.detectadaEm = pendencia.detectadaEm();
        entidade.resolvidaEm = pendencia.resolvidaEm();
        entidade.peca = peca;
        return entidade;
    }

    PendenciaPeca paraDominio() {
        return new PendenciaPeca(id, ordemServicoId, pecaId, peca.nome(),
                quantidadeFaltante, detectadaEm, resolvidaEm);
    }
}
