package com.artha.app.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:artha}")
    private String appName;

    @Bean
    public OpenAPI arthaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Artha API")
                        .description("REST API for the Artha personal finance & portfolio manager.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Abhishek Shukla")
                                .url("https://github.com/abhi03241"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local dev"),
                        new Server().url("http://13.201.204.129:8080").description("Live EC2")))
                .components(new Components());
    }
}