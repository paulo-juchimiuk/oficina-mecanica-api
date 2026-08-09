package br.com.oficinamecanica.catalogo.infrastructure;

import br.com.oficinamecanica.catalogo.domain.Dinheiro;
import br.com.oficinamecanica.catalogo.domain.Servico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "servico")
class ServicoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "valor_mao_de_obra", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMaoDeObra;

    @Column(name = "moeda", nullable = false, length = 3)
    private String moeda;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    protected ServicoJpaEntity() {
    }

    static ServicoJpaEntity de(Servico servico) {
        ServicoJpaEntity entidade = new ServicoJpaEntity();
        entidade.id = servico.id();
        entidade.nome = servico.nome();
        entidade.descricao = servico.descricao();
        entidade.valorMaoDeObra = servico.valorMaoDeObra().valor();
        entidade.moeda = servico.valorMaoDeObra().moeda();
        entidade.ativo = servico.ativo();
        return entidade;
    }

    Servico paraDominio() {
        return new Servico(id, nome, descricao, new Dinheiro(valorMaoDeObra, moeda), ativo);
    }
}
