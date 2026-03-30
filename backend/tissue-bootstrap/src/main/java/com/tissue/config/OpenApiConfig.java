package com.tissue.config;

import com.tissue.security.adapter.web.annotation.PublicApi;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Collections;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI tissueOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tissue API")
                        .description("Managing issues in the terminal")
                        .version("v0.7.0")
                        .license(new License().name("GPL-3.0").url("https://www.gnu.org/licenses/gpl-3.0.html")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public OperationCustomizer publicApiSecurityCustomizer() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.getBeanType().isAnnotationPresent(PublicApi.class)
                    || handlerMethod.hasMethodAnnotation(PublicApi.class)) {
                operation.setSecurity(Collections.emptyList());
            }
            return operation;
        };
    }
}
