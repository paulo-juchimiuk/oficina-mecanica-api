package br.com.oficinamecanica.autenticacao.infrastructure;

import br.com.oficinamecanica.shared.api.ApiPathPrefixConfig;
import br.com.oficinamecanica.shared.api.ErroResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@OpenAPIDefinition(
        info = @Info(title = "API da Oficina Mecanica", version = "1.0.0"),
        security = @SecurityRequirement(name = SecurityConfig.ESQUEMA_JWT))
@SecurityScheme(name = SecurityConfig.ESQUEMA_JWT, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class SecurityConfig {

    static final String ESQUEMA_JWT = "jwtAdministrativo";

    private static final String ROTA_DE_LOGIN = ApiPathPrefixConfig.PREFIXO + "/auth/login";
    private static final String ROTAS_DE_ACOMPANHAMENTO = ApiPathPrefixConfig.PREFIXO + "/acompanhamento/**";
    private static final String[] ROTAS_DA_DOCUMENTACAO = {"/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"};

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, AuthenticationEntryPoint entryPoint) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(ROTA_DE_LOGIN).permitAll()
                        .requestMatchers(ROTAS_DE_ACOMPANHAMENTO).permitAll()
                        .requestMatchers(ROTAS_DA_DOCUMENTACAO).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(entryPoint))
                .exceptionHandling(erros -> erros.authenticationEntryPoint(entryPoint))
                .build();
    }

    @Bean
    AuthenticationEntryPoint entryPointComCorpoDeErro(ObjectMapper objectMapper) {
        return (requisicao, resposta, excecao) -> {
            resposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resposta.setCharacterEncoding("UTF-8");
            ErroResponse corpo = new ErroResponse("NAO_AUTORIZADO", "JWT ausente, invalido ou expirado");
            objectMapper.writeValue(resposta.getOutputStream(), corpo);
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
