package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.CodigoAcompanhamento;
import br.com.oficinamecanica.ordemservico.domain.OrdemServico;
import br.com.oficinamecanica.ordemservico.domain.StatusOrdemServico;
import br.com.oficinamecanica.ordemservico.domain.TransicaoStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ordem_servico")
class OrdemServicoJpaEntity {

    private static final int TAMANHO_DO_STATUS = 24;
    private static final int TAMANHO_DO_CODIGO = 64;
    private static final int TAMANHO_DO_RELATO = 1000;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "veiculo_id", nullable = false)
    private UUID veiculoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = TAMANHO_DO_STATUS)
    private StatusOrdemServico status;

    @Column(name = "codigo_acompanhamento", nullable = false, unique = true, length = TAMANHO_DO_CODIGO)
    private String codigoAcompanhamento;

    @Column(name = "relato_do_problema", length = TAMANHO_DO_RELATO)
    private String relatoDoProblema;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("dataHora ASC")
    private List<TransicaoStatusJpaEntity> transicoes = new ArrayList<>();

    protected OrdemServicoJpaEntity() {
    }

    static OrdemServicoJpaEntity de(OrdemServico ordemServico) {
        OrdemServicoJpaEntity entidade = new OrdemServicoJpaEntity();
        entidade.id = ordemServico.id();
        entidade.clienteId = ordemServico.clienteId();
        entidade.veiculoId = ordemServico.veiculoId();
        entidade.codigoAcompanhamento = ordemServico.codigoAcompanhamento().valor();
        entidade.relatoDoProblema = ordemServico.relatoDoProblema();
        entidade.criadaEm = ordemServico.criadaEm();
        entidade.sincronizar(ordemServico);
        return entidade;
    }

    void sincronizar(OrdemServico ordemServico) {
        this.status = ordemServico.status();
        List<TransicaoStatus> registradas = ordemServico.transicoes();
        for (int posicao = transicoes.size(); posicao < registradas.size(); posicao++) {
            transicoes.add(TransicaoStatusJpaEntity.de(registradas.get(posicao), this));
        }
    }

    OrdemServico paraDominio() {
        return new OrdemServico(id, clienteId, veiculoId, status, new CodigoAcompanhamento(codigoAcompanhamento),
                relatoDoProblema, criadaEm, transicoes.stream().map(TransicaoStatusJpaEntity::paraDominio).toList());
    }
}
