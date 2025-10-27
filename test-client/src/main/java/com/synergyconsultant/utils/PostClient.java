package com.synergyconsultant.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PostClient {

    private final RestTemplate restTemplate;

    @Value("${user.id}")  // Инжектируем userId из свойств
    private String userId;

    public PostClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendMessage(String message, String url) {
        try {
            // Подготовка JSON-тела с userId
            String jsonBody = "{\"userId\": \"" + userId + "\", \"message\": \"" + message + "\"}";

            // Заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            // Отправка POST-запроса
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // Получаем тело ответа
            String responseBody = response.getBody();

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Сообщение отправлено успешно: " + message);
                if (responseBody != null && !responseBody.isEmpty()) {
                    System.out.println("Ответ от диспетчера: " + responseBody);
                }
            } else {
                System.out.println("Ошибка при отправке: " + response.getStatusCode());
                if (responseBody != null && !responseBody.isEmpty()) {
                    System.out.println("Детали ошибки от диспетчера: " + responseBody);
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка при отправке сообщения: " + e.getMessage());
        }
    }
}
