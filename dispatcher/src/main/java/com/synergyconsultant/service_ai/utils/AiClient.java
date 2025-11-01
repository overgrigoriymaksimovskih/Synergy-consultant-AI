package com.synergyconsultant.service_ai.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class AiClient {

    private final WebClient webClient;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.agent.access.id}")
    private String agentAccessId;

//    @Value("${ai.api.authorization}")
//    private String authorizationHeader;

    @Value("${ai.max.completion.tokens}")
    private int maxCompletionTokens;

    @Value("${ai.system.prompt}")
    private String aiSystemPrompt;

    public AiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Отправляет запрос к AI API и возвращает сырой JSON-ответ как CompletableFuture<String>.
     * @param messages Список сообщений для контекста.
     * @return CompletableFuture с сырым ответом от API.
     */
    public CompletableFuture<String> getAnswerFromAi(List<Map<String, Object>> messages) {
        // Создаём копию сообщений и добавляем system prompt как первую запись, это позволит кэшировать системный промпт
        // и не тратить на него токены
        List<Map<String, Object>> messagesWithSystem = new ArrayList<>(messages);
        if (aiSystemPrompt != null && !aiSystemPrompt.trim().isEmpty()) {
            messagesWithSystem.add(0, Map.of("role", "system", "content", aiSystemPrompt));
            System.out.println("Added system prompt to messages. First message: " + aiSystemPrompt.substring(0, Math.min(50, aiSystemPrompt.length())) + "...");
        } else {
            System.out.println("System prompt is empty, sending without system message.");
        }

        String url = apiUrl.replace("{agent_access_id}", agentAccessId);
        System.out.println("API URL: " + url);

        Map<String, Object> requestBody = Map.of(
                //Отправляем всю историю с добавленным в начало системным промптом
                "messages", messagesWithSystem,
                "max_completion_tokens", maxCompletionTokens,
                "stream", false
        );
        System.out.println("Request body: " + requestBody);

        Mono<String> responseMono = webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
//                .header("Authorization", authorizationHeader)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(rawResponse -> System.out.println("Raw response from AI API: " + rawResponse))
                .doOnError(error -> System.out.println("Error calling AI API: " + error.getMessage()))
                .onErrorResume(e -> {
                    System.out.println("Resuming from error: " + e.getMessage());
                    return Mono.just("{\"error\": \"Ошибка при вызове AI: " + e.getMessage() + "\"}");
                });

        return responseMono.toFuture();
    }
}