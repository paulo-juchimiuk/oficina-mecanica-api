package br.com.oficinamecanica.autenticacao.infrastructure;

import br.com.oficinamecanica.autenticacao.application.AutenticarUsuarioUseCase;
import br.com.oficinamecanica.autenticacao.application.EmissorDeToken;
import br.com.oficinamecanica.autenticacao.application.VerificadorDeSenha;
import br.com.oficinamecanica.autenticacao.domain.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CasosDeUsoDaAutenticacaoConfig {

    @Bean
    AutenticarUsuarioUseCase autenticarUsuarioUseCase(UsuarioRepository usuarios, VerificadorDeSenha verificadorDeSenha, EmissorDeToken emissorDeToken) {
        return new AutenticarUsuarioUseCase(usuarios, verificadorDeSenha, emissorDeToken);
    }
}
