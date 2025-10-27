package com.synergyconsultant.service;

import com.synergyconsultant.utils.PostClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class TestClient implements CommandLineRunner {

    @Value("${dispatcher.server.host}:${dispatcher.server.port}${dispatcher.server.address}")
    private String DISPATCHER_URL;
    private final PostClient postClient;
    public TestClient(PostClient postClient) {
        this.postClient = postClient;
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите сообщения. Для выхода введите 'exit'.");

        while (true) {
            System.out.print("Сообщение: ");
            String message = scanner.nextLine();

            if ("exit".equalsIgnoreCase(message)) {
                System.out.println("Выход из программы.");
                break;
            }

            // Отправка сообщения через PostClient
            postClient.sendMessage(message, DISPATCHER_URL);
        }

        scanner.close();
    }
}

