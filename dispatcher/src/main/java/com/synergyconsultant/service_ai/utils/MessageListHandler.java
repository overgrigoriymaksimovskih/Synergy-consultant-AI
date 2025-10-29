package com.synergyconsultant.service_ai.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyconsultant.dto.MessageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MessageListHandler {

    // История сообщений для контекста (ограничена 10 для простоты)
    private final List<Map<String, Object>> messageHistory = new ArrayList<>();
    private final ObjectMapper objectMapper;

    public MessageListHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Извлекает сообщение из запроса пользователя.
     * @param request Запрос от пользователя.
     * @return Map с ролью и содержимым сообщения.
     */
    public Map<String, Object> getMessageFromUserRequest(MessageRequest request) {
        return Map.of("role", "user", "content", request.getMessage());
    }

    /**
     * Извлекает сообщение из сырового JSON-ответа AI (синхронно).
     * @param responseJson Сырой JSON-ответ от AI.
     * @return Map с ролью и содержимым сообщения, или null если ошибка или ответ был обрезан.
     */
    public Map<String, Object> getMessageFromAiResponse(String responseJson) {
        try {
            if (responseJson == null || responseJson.trim().isEmpty()) {
                System.out.println("Empty response from API");
                return null;
            }

            Map<String, String> extracted = extractContentFromJson(responseJson);
            String content = extracted.get("content");
            String finishReason = extracted.get("finish_reason");

            System.out.println("Extracted content: " + content);
            System.out.println("Finish reason: " + finishReason);

            // Если content пустой и причина - лимит, возвращаем null (обработка в ProcessingService)
            if (content.trim().isEmpty() && "length".equals(finishReason)) {
                return null;
            }

            return Map.of("role", "assistant", "content", content);
        } catch (Exception e) {
            System.out.println("Error parsing AI response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Добавляет сообщение в историю и ограничивает размер списка.
     * @param message Сообщение для добавления.
     * @return Обновленный список сообщений.
     */
    public List<Map<String, Object>> addMessageToMessageList(Map<String, Object> message) {
        messageHistory.add(message);
        if (messageHistory.size() > 10) {
            messageHistory.remove(0);
        }
        return new ArrayList<>(messageHistory); // Возвращаем копию для безопасности
    }

    /**
     * Вспомогательный метод для извлечения контента и finish_reason из JSON с использованием ObjectMapper.
     * @param json Сырой JSON-ответ от AI.
     * @return Map с ключами "content" и "finish_reason".
     */
    private Map<String, String> extractContentFromJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.has("choices") && root.get("choices").size() > 0) {
                JsonNode choice = root.get("choices").get(0);
                String content = choice.path("message").path("content").asText();
                String finishReason = choice.path("finish_reason").asText();
                return Map.of("content", content, "finish_reason", finishReason);
            } else {
                System.out.println("No choices in response: " + json);
                return Map.of("content", "", "finish_reason", "");
            }
        } catch (Exception e) {
            System.out.println("Error extracting content from JSON: " + e.getMessage());
            return Map.of("content", "", "finish_reason", "");
        }
    }
}