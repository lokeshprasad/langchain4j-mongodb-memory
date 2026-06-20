package com.langchain4j.memory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaEmbeddingService implements EmbeddingService {

    private final String baseUrl;
    private final String modelName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);

    public OllamaEmbeddingService(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:llama3:latest}") String modelName) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public double[] embed(String text) {
        if (text == null || text.isBlank()) return new double[0];

        try {
            log.debug("Requesting embedding from Ollama model='{}' for text start='{}'", modelName, text.length() > 100 ? text.substring(0, 100) : text);
            String url = baseUrl;
            if (!url.endsWith("/")) url += "/";
            url += "embeddings";

            String body = objectMapper.writeValueAsString(
                    new java.util.HashMap<>() {{
                        put("model", modelName);
                        put("input", text);
                    }}
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("Ollama embeddings response status: {}", response.statusCode());
                JsonNode root = objectMapper.readTree(response.body());
                // Ollama embeddings response may contain data[0].embedding
                JsonNode dataNode = root.path("data");
                if (dataNode.isArray() && dataNode.size() > 0) {
                    JsonNode embNode = dataNode.get(0).path("embedding");
                    if (embNode.isArray()) {
                        double[] emb = new double[embNode.size()];
                        for (int i = 0; i < embNode.size(); i++) emb[i] = embNode.get(i).asDouble();
                        return emb;
                    }
                }
                // Some Ollama versions return 'embedding' directly
                JsonNode embNode = root.path("embedding");
                if (embNode.isArray()) {
                        log.debug("Ollama returned embedding array of size {}", embNode.size());
                    double[] emb = new double[embNode.size()];
                    for (int i = 0; i < embNode.size(); i++) emb[i] = embNode.get(i).asDouble();
                    return emb;
                }
            }
        } catch (Exception e) {
                log.warn("Ollama embedding call failed, falling back to local embedding: {}", e.getMessage());
        }

        // Fallback to local lightweight embedding
        return EmbeddingUtil.embed(text);
    }
}
