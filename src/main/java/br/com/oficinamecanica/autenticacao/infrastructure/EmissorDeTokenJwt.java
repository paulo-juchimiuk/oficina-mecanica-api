package br.com.oficinamecanica.autenticacao.infrastructure;

import br.com.oficinamecanica.autenticacao.application.EmissorDeToken;
import br.com.oficinamecanica.autenticacao.application.TokenJwt;
import br.com.oficinamecanica.autenticacao.domain.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;

@Component
class EmissorDeTokenJwt implements EmissorDeToken {

    private static final String EMISSOR = "oficina-mecanica-api";
    private static final String CLAIM_PERFIL = "perfil";

    private final JwtEncoder encoder;
    private final Duration validade;

    EmissorDeTokenJwt(JwtEncoder encoder, @Value("${oficina.jwt.validade-em-minutos}") long validadeEmMinutos) {
        this.encoder = encoder;
        this.validade = Duration.ofMinutes(validadeEmMinutos);
    }

    @Override
    public TokenJwt emitir(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(validade);
        JwtClaimsSet reivindicacoes = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuario.login())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim(CLAIM_PERFIL, usuario.perfil())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecalho, reivindicacoes)).getTokenValue();
        return new TokenJwt(token, expiraEm);
    }
}
