package br.com.oficinamecanica.autenticacao.infrastructure;

import br.com.oficinamecanica.autenticacao.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "usuario")
class UsuarioJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "login", nullable = false, length = 60)
    private String login;

    @Column(name = "senha_hash", nullable = false, length = 60)
    private String senhaHash;

    @Column(name = "perfil", nullable = false, length = 20)
    private String perfil;

    protected UsuarioJpaEntity() {
    }

    Usuario paraDominio() {
        return new Usuario(id, login, senhaHash, perfil);
    }
}
