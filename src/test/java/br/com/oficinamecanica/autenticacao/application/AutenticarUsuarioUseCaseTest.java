package br.com.oficinamecanica.autenticacao.application;

import br.com.oficinamecanica.autenticacao.domain.CredenciaisInvalidasException;
import br.com.oficinamecanica.autenticacao.domain.Usuario;
import br.com.oficinamecanica.autenticacao.domain.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Autenticacao de usuario administrativo")
class AutenticarUsuarioUseCaseTest {

    private static final String HASH = "$2b$10$UixwZYmjQ4G4ThX0XfLJfe3a3/cOV.0z9Cce5pLuARNhplyzvgMae";
    private static final Usuario ADMIN = new Usuario(UUID.randomUUID(), "admin", HASH, "ADMINISTRADOR");

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private VerificadorDeSenha verificadorDeSenha;

    @Mock
    private EmissorDeToken emissorDeToken;

    @InjectMocks
    private AutenticarUsuarioUseCase autenticarUsuario;

    @Test
    @DisplayName("deve emitir token quando login e senha conferem")
    void deveEmitirTokenComCredenciaisValidas() {
        TokenJwt esperado = new TokenJwt("token-assinado", Instant.now());
        when(usuarios.buscarPorLogin("admin")).thenReturn(Optional.of(ADMIN));
        when(verificadorDeSenha.confere("admin123", HASH)).thenReturn(true);
        when(emissorDeToken.emitir(ADMIN)).thenReturn(esperado);

        assertThat(autenticarUsuario.executar("admin", "admin123")).isEqualTo(esperado);
    }

    @Test
    @DisplayName("deve recusar login inexistente sem emitir token")
    void deveRecusarLoginInexistente() {
        when(usuarios.buscarPorLogin("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autenticarUsuario.executar("fantasma", "qualquer"))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(emissorDeToken, never()).emitir(ADMIN);
    }

    @Test
    @DisplayName("deve recusar senha errada sem emitir token")
    void deveRecusarSenhaErrada() {
        when(usuarios.buscarPorLogin("admin")).thenReturn(Optional.of(ADMIN));
        when(verificadorDeSenha.confere("errada", HASH)).thenReturn(false);

        assertThatThrownBy(() -> autenticarUsuario.executar("admin", "errada"))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(emissorDeToken, never()).emitir(ADMIN);
    }
}
