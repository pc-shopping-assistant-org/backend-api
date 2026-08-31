package com.ecm.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ecmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ECM Backend API")
                        .description("E-commerce API for the PC shopping assistant")
                        .version("v1")
                        .contact(new Contact().name("PC Shopping Assistant")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    /**
     * Keep Swagger's Authorize button accurate without marking public catalog
     * and webhook endpoints as protected.  Existing controller-level and
     * method-level {@link PreAuthorize} annotations are the authorization
     * source used for this documentation hint.
     */
    @Bean
    public OperationCustomizer securityOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (hasPreAuthorize(handlerMethod)) {
                operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
            }
            return operation;
        };
    }

    private boolean hasPreAuthorize(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(PreAuthorize.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
    }
}
