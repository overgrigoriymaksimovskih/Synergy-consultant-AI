package com.synergyconsultant.service_ai;

import com.synergyconsultant.dto.MessageRequest;
import com.synergyconsultant.service_ai.utils.AiClient;
import com.synergyconsultant.service_ai.utils.MessageListHandler;
import com.synergyconsultant.service_ai.utils.MessageListSummer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Profile("ai")
public class ProcessingService {

    @Value("${message.history.summer}")
    private Integer messageHistorySummer;

    @Value("${message.history.max}")
    private Integer messageHistoryMax;




    private final AiClient aiClient;
    private final MessageListHandler messageListHandler;
    private final MessageListSummer messageListSummer;
    private final List<Map<String, Object>> messageHistory = new ArrayList<>();

    public ProcessingService(AiClient aiClient, MessageListHandler messageListHandler, MessageListSummer messageListSummer) {
        this.aiClient = aiClient;
        this.messageListHandler = messageListHandler;
        this.messageListSummer = messageListSummer;
        messageHistory.add(Map.of("role", "user", "content", "чат начался "));
    }

    public CompletableFuture<String> processAsync(MessageRequest request) {
        // Проверяем размер истории перед добавлением, он может быть messageHistoryMax и больше если операция суммеризации еще не завершилась
        if (messageHistory.size() >= messageHistoryMax) {
            // Возвращаем сообщение о ожидании, не добавляя user-сообщение
            return CompletableFuture.completedFuture("{\"message\": \"Пожалуйста, подожди немного, я обрабатываю предыдущие сообщения.\"}");
        }

        // Получаем сообщение от пользователя
        Map<String, Object> userMessage = messageListHandler.getMessageFromUserRequest(request);

        // Добавляем в историю
        messageHistory.add(userMessage);
        System.out.println("Added user message. Message history size: " + messageHistory.size());
        logHistory();

        // Отправляем запрос к AI
        return aiClient.getAnswerFromAi(new ArrayList<>(messageHistory))
                .thenApply(rawResponse -> {
                    // Извлекаем сообщение из ответа AI
                    Map<String, Object> aiMessage = messageListHandler.getMessageFromAiResponse(rawResponse);
                    if (aiMessage != null && !aiMessage.get("content").toString().trim().isEmpty()) {
                        // Добавляем ответ AI в историю
                        messageHistory.add(aiMessage);
                        System.out.println("Added AI response. Message history size: " + messageHistory.size());
                        logHistory();

                        // После добавления, если >=6, запускаем суммаризацию в фоне и обновляем историю
                        if (messageHistory.size() >= messageHistoryMax) {
                            messageListSummer.summarizeIfNeeded(new ArrayList<>(messageHistory), messageHistorySummer, messageHistoryMax)
                                    .thenAccept(newHistory -> {
                                        synchronized (this) {
                                            messageHistory.clear();
                                            messageHistory.addAll(newHistory);
                                            System.out.println("History updated after summarization, new size: " + messageHistory.size());
                                            logHistory();
                                        }
                                    });
                        }

                        return "{\"message\": \"" + aiMessage.get("content").toString().replace("\"", "\\\"") + "\"}";
                    } else {
                        return "{\"error\": \"Ответ был обрезан или пустой.\"}";
                    }
                });
    }

    // Вспомогательный метод для логирования истории в читаемом виде
    private void logHistory() {
        System.out.println("Current message history:");
        for (int i = 0; i < messageHistory.size(); i++) {
            Map<String, Object> msg = messageHistory.get(i);
            System.out.println("  [" + (i + 1) + "] Role: " + msg.get("role") + ", Content: " + msg.get("content"));
        }
        System.out.println("--- End of history ---");
    }
}
