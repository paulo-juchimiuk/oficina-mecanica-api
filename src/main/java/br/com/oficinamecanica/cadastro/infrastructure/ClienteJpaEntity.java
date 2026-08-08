package br.com.oficinamecanica.cadastro.infrastructure;

import br.com.oficinamecanica.cadastro.domain.Cliente;
import br.com.oficinamecanica.cadastro.domain.Contato;
import br.com.oficinamecanica.cadastro.domain.Documento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "cliente")
class ClienteJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "documento", nullable = false, length = 14)
    private String documento;

    @Column(name = "email", nullable = false, length = 120)
    private String email;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    protected ClienteJpaEntity() {
    }

    static ClienteJpaEntity de(Cliente cliente) {
        ClienteJpaEntity entidade = new ClienteJpaEntity();
        entidade.id = cliente.id();
        entidade.nome = cliente.nome();
        entidade.documento = cliente.documento().numero();
        entidade.email = cliente.contato().email();
        entidade.telefone = cliente.contato().telefone();
        entidade.ativo = cliente.ativo();
        return entidade;
    }

    Cliente paraDominio() {
        return new Cliente(id, nome, new Documento(documento), new Contato(email, telefone), ativo);
    }
}
