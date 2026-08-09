package br.com.oficinamecanica.estoque.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.time.LocalDateTime;
import java.util.UUID;

public class PendenciaPeca {

    private static final String CODIGO = "PENDENCIA_INVALIDA";

    private final UUID id;
    private final UUID ordemServicoId;
    private final UUID pecaId;
    private final String nomePeca;
    private final int quantidadeFaltante;
    private final LocalDateTime detectadaEm;
    private final LocalDateTime resolvidaEm;

    public PendenciaPeca(UUID id, UUID ordemServicoId, UUID pecaId, String nomePeca,
                         int quantidadeFaltante, LocalDateTime detectadaEm, LocalDateTime resolvidaEm) {
        if (id == null || ordemServicoId == null || pecaId == null) {
            throw new DadosInvalidosException(CODIGO, "Pendencia de peca exige identidade, Ordem de Servico e Peca");
        }
        if (quantidadeFaltante <= 0) {
            throw new DadosInvalidosException(CODIGO, "Pendencia de peca exige quantidade faltante positiva");
        }
        this.id = id;
        this.ordemServicoId = ordemServicoId;
        this.pecaId = pecaId;
        this.nomePeca = nomePeca;
        this.quantidadeFaltante = quantidadeFaltante;
        this.detectadaEm = detectadaEm;
        this.resolvidaEm = resolvidaEm;
    }

    static PendenciaPeca registrar(UUID ordemServicoId, Peca peca, int quantidadeFaltante) {
        return new PendenciaPeca(UUID.randomUUID(), ordemServicoId, peca.id(), peca.nome(),
                quantidadeFaltante, LocalDateTime.now(), null);
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

    public int quantidadeFaltante() {
        return quantidadeFaltante;
    }

    public LocalDateTime detectadaEm() {
        return detectadaEm;
    }

    public LocalDateTime resolvidaEm() {
        return resolvidaEm;
    }
}
