package com.synergyconsultant.mockservice;

import com.synergyconsultant.dto.MessageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ProcessingService {

    @Async
    public CompletableFuture<String> processAsync(MessageRequest request) {
        try {
            // Имитация обработки (например, вызов AI или внешнего сервиса)
            System.out.println("Начинаем обработку для userId: " + request.getUserId());
            Thread.sleep(5000); // Задержка 5 секунд (имитация)

            // Генерируем ответ (можно добавить логику AI здесь)
            String responseMessage = "сообщение от пользователя " + request.getUserId() + " \"" + request.getMessage() + "\" " + " успешно обработано и готово к отправке";
            String jsonResponse = "{\"message\": \"" + responseMessage + "\"}";

            System.out.println("Обработка завершена для userId: " + request.getUserId());
            return CompletableFuture.completedFuture(jsonResponse);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture("{\"error\": \"Обработка прервана\"}");
        }
    }
}

