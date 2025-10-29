package com.synergyconsultant.service_ai;

import com.synergyconsultant.dto.MessageRequest;
import com.synergyconsultant.service_ai.utils.AiClient;
import com.synergyconsultant.service_ai.utils.MessageListHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Profile("ai")
public class ProcessingService {

    private final AiClient aiClient;
    private final MessageListHandler messageListHandler;

    public ProcessingService(AiClient aiClient, MessageListHandler messageListHandler) {
        this.aiClient = aiClient;
        this.messageListHandler = messageListHandler;
    }

    public CompletableFuture<String> processAsync(MessageRequest request) {
        // Получаем сообщение от пользователя
        Map<String, Object> userMessage = messageListHandler.getMessageFromUserRequest(request);
        // Добавляем в историю
        List<Map<String, Object>> updatedHistory = messageListHandler.addMessageToMessageList(userMessage);

        System.out.println("Processing message: " + request.getMessage());
        System.out.println("Message history size: " + updatedHistory.size());

        // Отправляем запрос к AI
        return aiClient.getAnswerFromAi(updatedHistory)
                .thenApply(rawResponse -> {
                    // Извлекаем сообщение из ответа AI
                    Map<String, Object> aiMessage = messageListHandler.getMessageFromAiResponse(rawResponse);
                    if (aiMessage != null && !aiMessage.get("content").toString().trim().isEmpty()) {
                        // Добавляем ответ AI в историю
                        messageListHandler.addMessageToMessageList(aiMessage);
                        return "{\"message\": \"" + aiMessage.get("content").toString().replace("\"", "\\\"") + "\"}";
                    } else {
                        return "{\"error\": \"Ответ был обрезан или пустой.\"}";
                    }
                });
    }
}