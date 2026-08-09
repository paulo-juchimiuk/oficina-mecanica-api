package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.time.LocalDateTime;
import java.util.UUID;

public class ReservaPeca {

    private static final String CODIGO = "RESERVA_INVALIDA";

    private final UUID id;
    private final UUID ordemServicoId;
    private final UUID pecaId;
    private final String nomePeca;
    private final int quantidade;
    private final LocalDateTime criadaEm;
    private SituacaoReserva situacao;

    public ReservaPeca(UUID id, UUID ordemServicoId, UUID pecaId, String nomePeca,
                       int quantidade, SituacaoReserva situacao, LocalDateTime criadaEm) {
        if (id == null || ordemServicoId == null || pecaId == null) {
            throw new DadosInvalidosException(CODIGO, "Reserva de peca exige identidade, Ordem de Servico e Peca");
        }
        if (quantidade <= 0) {
            throw new DadosInvalidosException(CODIGO, "Reserva de peca exige quantidade positiva");
        }
        if (situacao == null) {
            throw new DadosInvalidosException(CODIGO, "Reserva de peca exige situacao");
        }
        this.id = id;
        this.ordemServicoId = ordemServicoId;
        this.pecaId = pecaId;
        this.nomePeca = nomePeca;
        this.quantidade = quantidade;
        this.situacao = situacao;
        this.criadaEm = criadaEm;
    }

    static ReservaPeca ativar(UUID ordemServicoId, Peca peca, int quantidade) {
        return new ReservaPeca(UUID.randomUUID(), ordemServicoId, peca.id(), peca.nome(),
                quantidade, SituacaoReserva.ATIVA, LocalDateTime.now());
    }

    void consumir() {
        if (situacao != SituacaoReserva.ATIVA) {
            throw new ReservaForaDaSituacaoEsperadaException(id, situacao, SituacaoReserva.ATIVA);
        }
        this.situacao = SituacaoReserva.CONSUMIDA;
    }

    void devolver() {
        if (situacao == SituacaoReserva.DEVOLVIDA) {
            throw new ReservaJaDevolvidaException(id);
        }
        this.situacao = SituacaoReserva.DEVOLVIDA;
    }

    public boolean estaAtiva() {
        return situacao == SituacaoReserva.ATIVA;
    }

    public boolean estaConsumida() {
        return situacao == SituacaoReserva.CONSUMIDA;
    }

    public UUID id() {
        return id;
    }

    public UUID ordemServicoId() {
        return ordemServicoId;
    }

    public UUID pecaId() {
        return pecaId;
    }

    public String nomePeca() {
        return nomePeca;
    }

    public int quantidade() {
        return quantidade;
    }

    public SituacaoReserva situacao() {
        return situacao;
    }

    public LocalDateTime criadaEm() {
        return criadaEm;
    }
}
