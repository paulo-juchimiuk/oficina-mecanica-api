package br.com.oficinamecanica.shared.api;

import br.com.oficinamecanica.OficinaMecanicaApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiPathPrefixConfig implements WebMvcConfigurer {

    public static final String PREFIXO = "/api/v1";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(PREFIXO, HandlerTypePredicate.forBasePackageClass(OficinaMecanicaApplication.class));
    }
}
