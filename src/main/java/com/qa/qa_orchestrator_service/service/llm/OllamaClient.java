package com.qa.qa_orchestrator_service.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OllamaClient
 *
 * LLM provider: Ollama (self-hosted, fully offline)
 * Active when: LLM_PROVIDER=ollama
 *
 * Supports any model running locally via Ollama:
 * - llama3.3 (recommended — matches Groq quality)
 * - mistral
 * - codellama
 * - phi3
 *
 * Required env vars:
 * - LLM_PROVIDER=ollama
 * - OLLAMA_BASE_URL=http://localhost:11434  (or internal server IP)
 * - OLLAMA_MODEL=llama3.3  (or any installed model)
 *
 * Install Ollama: https://ollama.com
 * Pull model: ollama pull llama3.3
 * Run: ollama serve
 *
 * Zero internet required after initial model download.
 * Ideal for air-gapped, on-premise, or government deployments.
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama")
public class OllamaClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:llama3.3}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaClient(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(120))  // local models can be slower
                .build();
    }

    @Override
    public String call(String systemPrompt, String userContent) {
        try {
            String url = baseUrl + "/api/chat";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "stream", false,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userContent)
                    ),
                    "options", Map.of(
                            "temperature", 0.2,
                            "num_predict", 2048
                    )
            );

            log.info("[OLLAMA] Calling model={} at {}", model, baseUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers), String.class);

            return extractText(response.getBody());

        } catch (Exception e) {
            log.error("[OLLAMA] Call failed: {}", e.getMessage());
            throw new RuntimeException("Ollama call failed: " + e.getMessage(), e);
        }
    }

    private String extractText(String responseBody) {
        try {
            return objectMapper.readTree(responseBody)
                    .path("message")
                    .path("content")
                    .asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Ollama response: " + e.getMessage(), e);
        }
    }
}