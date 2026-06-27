package com.artha.app.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Real OpenAI Chat Completions client using Spring's {@link RestClient}.
 * Activated only when {@code artha.ai.openai.enabled=true} AND an api key is set.
 *
 * Returns the assistant message content, or null on any failure (network, 4xx/5xx,
 * malformed response) — never throws. Designed to be a drop-in replacement for
 * {@link StubAiClient}.
 */
@Component
@ConditionalOnProperty(prefix = "artha.ai.openai", name = "enabled", havingValue = "true")
public class OpenAiHttpClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiHttpClient.class);

    private final RestClient http;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiHttpClient(@Value("${openai.api.key:}") String apiKey,
                            @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}") String baseUrl,
                            @Value("${artha.ai.openai.model:gpt-4o-mini}") String model,
                            ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.http = RestClient.builder()
                .baseUrl(baseUrl.substring(0, baseUrl.lastIndexOf("/chat/completions")))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                            Map.of("role", "user",   "content", userPrompt == null ? "" : userPrompt)
                    ),
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object")
            );
            String response = http.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() || content.isNull() ? null : content.asText();
        } catch (Exception ex) {
            log.warn("OpenAI completion failed: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public String name() {
        return "openai:" + model;
    }
}