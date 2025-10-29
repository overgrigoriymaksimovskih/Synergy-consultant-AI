package com.synergyconsultant.service_ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyconsultant.dto.MessageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Profile("ai")
public class ProcessingService {

    private final WebClient webClient;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.agent.access.id}")
    private String agentAccessId;

    @Value("${ai.api.authorization}")
    private String authorizationHeader;

    @Value("${ai.max.completion.tokens}")  // Увеличен по умолчанию до 1000, настройте в properties
    private int maxCompletionTokens;

    private final ObjectMapper objectMapper;

    // История сообщений для контекста (ограничена 10 для простоты)
    private final List<Map<String, Object>> messageHistory = new ArrayList<>();

    public ProcessingService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<String> processAsync(MessageRequest request) {
        // Добавляем пользовательское сообщение в историю
        messageHistory.add(Map.of("role", "user", "content", request.getMessage()));
        // Ограничиваем историю (последние 10 сообщений)
        if (messageHistory.size() > 10) {
            messageHistory.remove(0);
        }

        System.out.println("Processing message: " + request.getMessage());
        System.out.println("Message history size: " + messageHistory.size());

        String url = apiUrl.replace("{agent_access_id}", agentAccessId);
        System.out.println("API URL: " + url);

        Map<String, Object> requestBody = Map.of(
                "messages", messageHistory,  // Теперь с историей
                "max_completion_tokens", maxCompletionTokens,
                "stream", false
        );
        System.out.println("Request body: " + requestBody);

        Mono<String> responseMono = webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Authorization", authorizationHeader)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(rawResponse -> System.out.println("Raw response from AI API: " + rawResponse))
                .doOnError(error -> System.out.println("Error calling AI API: " + error.getMessage()))
                .map(this::extractMessageFromResponse)
                .onErrorResume(e -> {
                    System.out.println("Resuming from error: " + e.getMessage());
                    return Mono.just("{\"error\": \"Ошибка при вызове AI: " + e.getMessage() + "\"}");
                });

        return responseMono.toFuture();
    }

    private String extractMessageFromResponse(String responseJson) {
        try {
            if (responseJson == null || responseJson.trim().isEmpty()) {
                System.out.println("Empty response from API");
                return "{\"message\": \"\"}";
            }
            JsonNode root = objectMapper.readTree(responseJson);
            if (root.has("choices") && root.get("choices").size() > 0) {
                JsonNode choice = root.get("choices").get(0);
                String content = choice.path("message").path("content").asText();
                String finishReason = choice.path("finish_reason").asText();

                System.out.println("Extracted content: " + content);
                System.out.println("Finish reason: " + finishReason);

                // Если content пустой и причина - лимит, возвращаем ошибку
                if (content.trim().isEmpty() && "length".equals(finishReason)) {
                    return "{\"error\": \"Ответ был обрезан из-за лимита токенов. Попробуйте уточнить вопрос или увеличить лимит.\"}";
                }

                // Добавляем ответ ассистента в историю (если не пустой)
                if (!content.trim().isEmpty()) {
                    messageHistory.add(Map.of("role", "assistant", "content", content));
                    if (messageHistory.size() > 10) {
                        messageHistory.remove(0);
                    }
                }

                return "{\"message\": \"" + content.replace("\"", "\\\"") + "\"}";
            } else {
                System.out.println("No choices in response: " + responseJson);
                return "{\"message\": \"\"}";
            }
        } catch (Exception e) {
            System.out.println("Error parsing response: " + e.getMessage());
            return "{\"error\": \"Ошибка парсинга ответа AI\"}";
        }
    }
}
