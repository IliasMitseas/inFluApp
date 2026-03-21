package org.ilias.influapp.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inFluAppOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("InFluApp API")
                        .description("API documentation for InFluApp")
                        .version("v0.0.1")
                        .contact(new Contact().name("InFluApp Team").email("noreply@influapp.local"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project docs")
                        .url("https://example.org/docs"));
    }
}
