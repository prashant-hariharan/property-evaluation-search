package com.prashant.propertysearch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Open AI / Swagger configuration
 * @author prashant
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI propertySearchOpenApi() {
        return new OpenAPI()
                .openapi("3.0.3")
                .info(new Info()
                        .title("Property Evaluation Search API")
                        .description("CRUD APIs for properties and evaluations. Used as foundation for Lucene and MariaDB full-text search comparison.")
                        .version("v1")
                        .contact(new Contact().name("Prashant Hariharan"))
                        .license(new License().name("Internal Use")));
    }
}
