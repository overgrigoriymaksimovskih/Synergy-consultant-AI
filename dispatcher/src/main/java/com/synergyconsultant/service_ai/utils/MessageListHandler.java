package com.synergyconsultant.service_ai.utils;

import com.synergyconsultant.dto.MessageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MessageListHandler {

    // История сообщений для контекста (ограничена 10 для простоты)
    private final List<Map<String, Object>> messageHistory = new ArrayList<>();

    public Map<String, Object> getMessageFromUserRequest(MessageRequest request) {
        //тут код получения сообщения из запроса с клиента
    }

    public Map<String, Object> getMessageFromAiResponse(CompletableFuture<String> response) {
        //тут код получения сообщения из ответа АИ модели
    }

    public List<Map<String, Object>> addMesageToMessageList(Map<String, Object> message) {
        //тут код добавления сообщения в список и возвращения обновленного списка
    }
}
