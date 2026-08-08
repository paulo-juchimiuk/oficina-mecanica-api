package br.com.oficinamecanica.autenticacao.infrastructure;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
class JwtConfig {

    private static final String ALGORITMO_DA_CHAVE = "HmacSHA256";
    private static final int BYTES_MINIMOS_DO_SEGREDO = 32;

    private final SecretKeySpec chave;

    JwtConfig(@Value("${oficina.jwt.segredo}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < BYTES_MINIMOS_DO_SEGREDO) {
            throw new IllegalStateException(
                    "oficina.jwt.segredo tem " + bytes.length + " bytes; HS256 exige no minimo " + BYTES_MINIMOS_DO_SEGREDO);
        }
        this.chave = new SecretKeySpec(bytes, ALGORITMO_DA_CHAVE);
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
