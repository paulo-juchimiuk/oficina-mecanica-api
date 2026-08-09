package br.com.oficinamecanica.estoque.infrastructure;

import br.com.oficinamecanica.estoque.domain.ReservaPeca;
import br.com.oficinamecanica.estoque.domain.SituacaoReserva;
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
@Table(name = "reserva_peca")
class ReservaPecaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "ordem_servico_id", nullable = false)
    private UUID ordemServicoId;

    @Column(name = "peca_id", nullable = false)
    private UUID pecaId;

    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 12)
    private SituacaoReserva situacao;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peca_id", insertable = false, updatable = false)
    private PecaJpaEntity peca;

    protected ReservaPecaJpaEntity() {
    }

    static ReservaPecaJpaEntity de(ReservaPeca reserva) {
        ReservaPecaJpaEntity entidade = new ReservaPecaJpaEntity();
        entidade.id = reserva.id();
        entidade.ordemServicoId = reserva.ordemServicoId();
        entidade.pecaId = reserva.pecaId();
        entidade.quantidade = reserva.quantidade();
        entidade.situacao = reserva.situacao();
        entidade.criadaEm = reserva.criadaEm();
        return entidade;
    }

    void mover(SituacaoReserva situacao) {
        this.situacao = situacao;
    }

    ReservaPeca paraDominio() {
        return new ReservaPeca(id, ordemServicoId, pecaId, peca.nome(), quantidade, situacao, criadaEm);
    }
}
