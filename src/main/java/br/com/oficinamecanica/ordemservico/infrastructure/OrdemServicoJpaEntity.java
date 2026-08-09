package br.com.oficinamecanica.ordemservico.infrastructure;

import br.com.oficinamecanica.ordemservico.domain.CodigoAcompanhamento;
import br.com.oficinamecanica.ordemservico.domain.ItemPeca;
import br.com.oficinamecanica.ordemservico.domain.ItemServico;
import br.com.oficinamecanica.ordemservico.domain.Orcamento;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("versao ASC")
    private List<OrcamentoJpaEntity> orcamentos = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItemServicoJpaEntity> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItemPecaJpaEntity> itensPeca = new ArrayList<>();

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
        sincronizarTransicoes(ordemServico.transicoes());
        sincronizarOrcamentos(ordemServico.orcamentos());
        sincronizarItens(ordemServico);
    }

    private void sincronizarTransicoes(List<TransicaoStatus> registradas) {
        for (int posicao = transicoes.size(); posicao < registradas.size(); posicao++) {
            transicoes.add(TransicaoStatusJpaEntity.de(registradas.get(posicao), this));
        }
    }

    private void sincronizarOrcamentos(List<Orcamento> versoes) {
        Map<Integer, OrcamentoJpaEntity> porVersao = orcamentosPorVersao();
        versoes.forEach(versao -> sincronizarVersao(porVersao, versao));
    }

    private void sincronizarVersao(Map<Integer, OrcamentoJpaEntity> porVersao, Orcamento versao) {
        OrcamentoJpaEntity persistida = porVersao.get(versao.versao());
        if (persistida == null) {
            orcamentos.add(OrcamentoJpaEntity.de(versao, this));
            return;
        }
        persistida.sincronizar(versao);
    }

    private void sincronizarItens(OrdemServico ordemServico) {
        Map<Integer, OrcamentoJpaEntity> porVersao = orcamentosPorVersao();
        Set<UUID> servicosGravados = itensServico.stream()
                .map(ItemServicoJpaEntity::id).collect(Collectors.toCollection(HashSet::new));
        Set<UUID> pecasGravadas = itensPeca.stream()
                .map(ItemPecaJpaEntity::id).collect(Collectors.toCollection(HashSet::new));

        ordemServico.itensServico().stream()
                .filter(item -> !servicosGravados.contains(item.id()))
                .forEach(item -> itensServico.add(
                        ItemServicoJpaEntity.de(item, this, porVersao.get(item.versaoOrigem()))));
        ordemServico.itensPeca().stream()
                .filter(item -> !pecasGravadas.contains(item.id()))
                .forEach(item -> itensPeca.add(
                        ItemPecaJpaEntity.de(item, this, porVersao.get(item.versaoOrigem()))));
    }

    private Map<Integer, OrcamentoJpaEntity> orcamentosPorVersao() {
        return orcamentos.stream().collect(Collectors.toMap(OrcamentoJpaEntity::versao, Function.identity()));
    }

    OrdemServico paraDominio() {
        return new OrdemServico(id, clienteId, veiculoId, status, new CodigoAcompanhamento(codigoAcompanhamento),
                relatoDoProblema, criadaEm,
                transicoes.stream().map(TransicaoStatusJpaEntity::paraDominio).toList(),
                orcamentos.stream().map(OrcamentoJpaEntity::paraDominio).toList(),
                itensServico.stream().map(ItemServicoJpaEntity::paraDominio).map(ItemServico.class::cast).toList(),
                itensPeca.stream().map(ItemPecaJpaEntity::paraDominio).map(ItemPeca.class::cast).toList());
    }
}
