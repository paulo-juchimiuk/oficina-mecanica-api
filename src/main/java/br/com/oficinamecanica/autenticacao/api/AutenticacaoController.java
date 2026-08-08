package br.com.oficinamecanica.autenticacao.api;

import br.com.oficinamecanica.autenticacao.application.AutenticarUsuarioUseCase;
import br.com.oficinamecanica.autenticacao.application.TokenJwt;
import br.com.oficinamecanica.autenticacao.domain.CredenciaisInvalidasException;
import br.com.oficinamecanica.shared.api.ErroResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    private final AutenticarUsuarioUseCase autenticarUsuario;

    public AutenticacaoController(AutenticarUsuarioUseCase autenticarUsuario) {
        this.autenticarUsuario = autenticarUsuario;
    }

    @PostMapping("/login")
    @SecurityRequirements
    public TokenJwt login(@Valid @RequestBody Credenciais credenciais) {
        return autenticarUsuario.executar(credenciais.login(), credenciais.senha());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> credenciaisInvalidas(CredenciaisInvalidasException excecao) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroResponse(excecao.codigo(), excecao.getMessage()));
    }
}
