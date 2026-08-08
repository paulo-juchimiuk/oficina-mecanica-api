package br.com.oficinamecanica.autenticacao.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Usuario")
class UsuarioTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String HASH = "$2b$10$UixwZYmjQ4G4ThX0XfLJfe3a3/cOV.0z9Cce5pLuARNhplyzvgMae";

    @Test
    @DisplayName("deve guardar login, hash da senha e perfil")
    void deveGuardarCredenciais() {
        Usuario usuario = new Usuario(ID, "admin", HASH, "ADMINISTRADOR");
        assertThat(usuario.login()).isEqualTo("admin");
        assertThat(usuario.senhaHash()).isEqualTo(HASH);
        assertThat(usuario.perfil()).isEqualTo("ADMINISTRADOR");
    }

    @Test
    @DisplayName("deve exigir identidade")
    void deveExigirIdentidade() {
        assertThatThrownBy(() -> new Usuario(null, "admin", HASH, "ADMINISTRADOR"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("identidade");
    }

    @Test
    @DisplayName("deve exigir login")
    void deveExigirLogin() {
        assertThatThrownBy(() -> new Usuario(ID, "  ", HASH, "ADMINISTRADOR"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("login");
    }

    @Test
    @DisplayName("deve exigir senha")
    void deveExigirSenha() {
        assertThatThrownBy(() -> new Usuario(ID, "admin", null, "ADMINISTRADOR"))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("senha");
    }

    @Test
    @DisplayName("deve exigir perfil")
    void deveExigirPerfil() {
        assertThatThrownBy(() -> new Usuario(ID, "admin", HASH, ""))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("perfil");
    }

    @Test
    @DisplayName("deve carregar o codigo de erro que a API devolve")
    void deveCarregarCodigoDeErro() {
        assertThatThrownBy(() -> new Usuario(ID, "admin", HASH, null))
                .isInstanceOf(DadosInvalidosException.class)
                .extracting(erro -> ((DadosInvalidosException) erro).codigo())
                .isEqualTo("USUARIO_INVALIDO");
    }
}
