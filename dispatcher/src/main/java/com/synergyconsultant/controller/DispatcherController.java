package com.synergyconsultant.controller;

import com.synergyconsultant.dto.MessageRequest;
import com.synergyconsultant.mockservice.ProcessingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/dispatcher")
public class DispatcherController {

    private final ProcessingService processingService;

    // Инъекция сервиса через конструктор
    public DispatcherController(ProcessingService processingService) {
        this.processingService = processingService;
    }

    @PostMapping
    public DeferredResult<ResponseEntity<String>> handleMessage(@RequestBody MessageRequest request) {
        DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>();

        // Проверка на валидность (как и раньше)
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            deferredResult.setResult(ResponseEntity.badRequest().body("{\"error\": \"userId не может быть пустым\"}"));
            return deferredResult;
        }
        if (request.getMessage() == null || request.getMessage().isEmpty()) {
            deferredResult.setResult(ResponseEntity.badRequest().body("{\"error\": \"message не может быть пустым\"}"));
            return deferredResult;
        }

        // Запускаем асинхронную обработку
        CompletableFuture<String> future = processingService.processAsync(request);
        future.thenAccept(result -> {
            // Когда обработка завершена, устанавливаем результат
            deferredResult.setResult(ResponseEntity.ok(result));
        }).exceptionally(ex -> {
            // Обработка ошибок
            deferredResult.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Внутренняя ошибка сервера: " + ex.getMessage() + "\"}"));
            return null;
        });

        return deferredResult;
    }
}
