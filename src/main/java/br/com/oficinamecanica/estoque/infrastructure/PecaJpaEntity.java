package br.com.oficinamecanica.estoque.infrastructure;

import br.com.oficinamecanica.estoque.domain.Dinheiro;
import br.com.oficinamecanica.estoque.domain.Peca;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "peca")
class PecaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "unidade_medida", nullable = false, length = 20)
    private String unidadeMedida;

    @Column(name = "preco", nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "moeda", nullable = false, length = 3)
    private String moeda;

    @Column(name = "saldo_em_estoque", nullable = false)
    private int saldoEmEstoque;

    @Column(name = "quantidade_reservada", nullable = false)
    private int quantidadeReservada;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    protected PecaJpaEntity() {
    }

    static PecaJpaEntity de(Peca peca) {
        PecaJpaEntity entidade = new PecaJpaEntity();
        entidade.id = peca.id();
        entidade.nome = peca.nome();
        entidade.unidadeMedida = peca.unidadeMedida();
        entidade.preco = peca.preco().valor();
        entidade.moeda = peca.preco().moeda();
        entidade.saldoEmEstoque = peca.saldoEmEstoque();
        entidade.quantidadeReservada = peca.quantidadeReservada();
        entidade.estoqueMinimo = peca.estoqueMinimo();
        entidade.ativo = peca.ativo();
        return entidade;
    }

    Peca paraDominio() {
        return new Peca(id, nome, unidadeMedida, new Dinheiro(preco, moeda),
                saldoEmEstoque, quantidadeReservada, estoqueMinimo, ativo);
    }

    String nome() {
        return nome;
    }
}
