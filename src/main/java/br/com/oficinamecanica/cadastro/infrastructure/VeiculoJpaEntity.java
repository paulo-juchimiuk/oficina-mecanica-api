package br.com.oficinamecanica.cadastro.infrastructure;

import br.com.oficinamecanica.cadastro.domain.Placa;
import br.com.oficinamecanica.cadastro.domain.Veiculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "veiculo")
class VeiculoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "placa", nullable = false, length = 7)
    private String placa;

    @Column(name = "marca", nullable = false, length = 60)
    private String marca;

    @Column(name = "modelo", nullable = false, length = 60)
    private String modelo;

    @Column(name = "ano", nullable = false)
    private int ano;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    protected VeiculoJpaEntity() {
    }

    static VeiculoJpaEntity de(Veiculo veiculo) {
        VeiculoJpaEntity entidade = new VeiculoJpaEntity();
        entidade.id = veiculo.id();
        entidade.placa = veiculo.placa().valor();
        entidade.marca = veiculo.marca();
        entidade.modelo = veiculo.modelo();
        entidade.ano = veiculo.ano();
        entidade.clienteId = veiculo.clienteId();
        entidade.ativo = veiculo.ativo();
        return entidade;
    }

    Veiculo paraDominio() {
        return new Veiculo(id, new Placa(placa), marca, modelo, ano, clienteId, ativo);
    }
}
