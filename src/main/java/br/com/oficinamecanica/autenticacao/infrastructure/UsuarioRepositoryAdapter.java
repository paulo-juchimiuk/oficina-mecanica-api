package br.com.oficinamecanica.autenticacao.infrastructure;

import br.com.oficinamecanica.autenticacao.domain.Usuario;
import br.com.oficinamecanica.autenticacao.domain.UsuarioRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository repository;

    UsuarioRepositoryAdapter(UsuarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Usuario> buscarPorLogin(String login) {
        return repository.findByLogin(login).map(UsuarioJpaEntity::paraDominio);
    }
}
