package com.synergyconsultant.service_ai.utils;

import com.synergyconsultant.service_ai.utils.AiClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class MessageListSummer {

    private final AiClient aiClient;

    public MessageListSummer(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    /**
     * Асинхронно суммаризирует первые 4 сообщения в истории, если размер >=6.
     * Возвращает новую историю: summary + последние 2, или текущую, если <6.
     * @param currentHistory Текущая история (копия).
     * @return CompletableFuture с новой историей.
     */
    public CompletableFuture<List<Map<String, Object>>> summarizeIfNeeded(List<Map<String, Object>> currentHistory, Integer messageHistorySummer, Integer messageHistoryMax) {
        if (currentHistory.size() < messageHistoryMax) {
            // Возвращаем текущую историю без изменений
            return CompletableFuture.completedFuture(currentHistory);
        }

        // Берём первые messageHistorySummer для суммаризации
        List<Map<String, Object>> firstFour = new ArrayList<>(currentHistory.subList(0, messageHistorySummer));
        StringBuilder conversationText = new StringBuilder("Summarize the following conversation in 1-2 sentences, keeping key context:\n");
        for (Map<String, Object> msg : firstFour) {
            conversationText.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
        }

        List<Map<String, Object>> summaryRequest = List.of(Map.of("role", "user", "content", conversationText.toString()));

        // Запускаем суммаризацию асинхронно и возвращаем новую историю
        return aiClient.getAnswerFromAi(summaryRequest)
                .thenApply(response -> {
                    try {
                        String summaryContent = extractSummaryFromResponse(response, messageHistoryMax);
                        if (summaryContent != null && !summaryContent.trim().isEmpty()) {
                            Map<String, Object> summaryMessage = Map.of("role", "system", "content", summaryContent);

                            // Создаём новую историю: summary + размер списка - messageHistorySummer
                            List<Map<String, Object>> newHistory = new ArrayList<>();
                            newHistory.add(summaryMessage);
                            newHistory.addAll(currentHistory.subList(messageHistorySummer, currentHistory.size())); // Последние 2
                            return newHistory;
                        } else {
                            System.out.println("Summarization failed: empty summary, returning current history");
                            return currentHistory;
                        }
                    } catch (Exception e) {
                        System.out.println("Error during summarization: " + e.getMessage() + ", returning current history");
                        return currentHistory;
                    }
                })
                .exceptionally(ex -> {
                    System.out.println("Exception in summarization: " + ex.getMessage() + ", returning current history");
                    return currentHistory;
                });
    }

    /**
     * Вспомогательный метод для извлечения summary из ответа AI (упрощённо, без ObjectMapper для краткости).
     * @param responseJson Ответ от AI.
     * @return Summary content или null.
     */
    private String extractSummaryFromResponse(String responseJson, Integer messageHistoryMax) {
        try {
            // Простой парсинг (предполагаем JSON как в AiClient)
            if (responseJson.contains("\"content\"")) {
                int start = responseJson.indexOf("\"content\":\"") +11;
                int end = responseJson.indexOf("\"", start);
                return responseJson.substring(start, end).replace("\\\"", "\"");
            }
        } catch (Exception e) {
            System.out.println("Error extracting summary: " + e.getMessage());
        }
        return null;
    }
}

