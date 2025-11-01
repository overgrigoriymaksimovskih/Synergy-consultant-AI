package com.synergyconsultant.service_weaviate;

import com.synergyconsultant.exceptions.WeaviateException;
import com.synergyconsultant.entity.ChatMessage;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.argument.NearTextArgument;
import io.weaviate.client.v1.graphql.query.fields.Field;
import io.weaviate.client.v1.misc.model.Meta;
import io.weaviate.client.v1.schema.model.WeaviateClass;  // Добавлен импорт для WeaviateClass
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;  // Для timestamp
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;  // Добавлен импорт для LinkedList

@Service
public class MemoryService {

    private final WeaviateClient weaviateClient;
    private final Queue<ChatMessage> localHistory = new LinkedList<>();

    @Value("${memory.service.local.history.size}")
    private int maxLocalHistorySize;

    @Value("${memory.service.weaviate.class}")
    private String weaviateClassName;

    @Value("${memory.service.max.messages.in.weaviate}")
    private int maxWeaviateMessages;

    @Autowired
    public MemoryService(WeaviateClient weaviateClient) {
        this.weaviateClient = weaviateClient;
        createClassIfNotExists();
    }

    public List<Map<String, Object>> enhanceMessageHistory(List<Map<String, Object>> history, String lastUserMessage) {
        List<Map<String, Object>> context = retrieveContext(lastUserMessage);
        List<Map<String, Object>> enhanced = new ArrayList<>();
        if (!history.isEmpty()) {
            enhanced.add(history.get(0));  // prompt
        }
        enhanced.addAll(context);
        enhanced.addAll(history.subList(1, history.size()));
        return enhanced;
    }

    public void saveMessage(String role, String content, String userId) {
        ChatMessage msg = new ChatMessage(role, content);  // Конструктор с userId, role, content (timestamp внутри класса)
        localHistory.add(msg);
        if (localHistory.size() > maxLocalHistorySize) {
            localHistory.poll();
        }
        syncLatestToWeaviate();
    }

    private List<Map<String, Object>> retrieveContext(String query) {
        try {
            Result<GraphQLResponse> result = weaviateClient.graphQL().get()
                    .withClassName(weaviateClassName)
                    .withFields(Field.builder()
                            .name("role")
                            .name("content")
                            .build())
                    .withNearText(NearTextArgument.builder()
                            .concepts(new String[]{query})
                            .build())
                    .withLimit(3)
                    .run();

            if (result.hasErrors()) {
                throw new WeaviateException("Ошибка поиска в Weaviate: " + result.getError().getMessages(), null);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            if (result.getResult() != null && result.getResult().getData() != null) {
                // Добавлен каст для getData()
                Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
                Map<String, Object> getSection = (Map<String, Object>) data.get("Get");
                if (getSection != null) {
                    List<Map<String, Object>> objects = (List<Map<String, Object>>) getSection.get(weaviateClassName);
                    if (objects != null) {
                        objects.forEach(obj -> {
                            // obj уже Map<String, Object> с ключами "role", "content"
                            String role = (String) obj.get("role");
                            String content = (String) obj.get("content");
                            if (role != null && content != null) {
                                results.add(Map.of("role", role, "content", content));
                            }
                        });
                    }
                }
            }
            return results;
        } catch (Exception e) {
            throw new WeaviateException("Ошибка при извлечении контекста: " + e.getMessage(), e);
        }
    }

    private void syncLatestToWeaviate() {
        List<ChatMessage> toSync = new ArrayList<>(localHistory);
        while (toSync.size() > maxWeaviateMessages) {
            toSync.remove(0);
        }

        try {
            weaviateClient.batch().objectsBatchDeleter().withClassName(weaviateClassName).run();
            for (ChatMessage msg : toSync) {
                Map<String, Object> properties = Map.of(
                        "role", msg.getRole(),
                        "content", msg.getContent(),
                        "timestamp", msg.getTimestamp()
                );
                weaviateClient.data().creator()
                        .withClassName(weaviateClassName)
                        .withProperties(properties)
                        .run();
            }
        } catch (Exception e) {
            throw new WeaviateException("Ошибка синхронизации с Weaviate: " + e.getMessage(), e);
        }
    }

    private void createClassIfNotExists() {
        try {
            Result<Meta> metaResult = weaviateClient.misc().metaGetter().run();
            if (metaResult.hasErrors()) {
                throw new RuntimeException("Weaviate не подключен");
            }

            // Заменил Map на WeaviateClass для совместимости с API
            WeaviateClass weaviateClass = WeaviateClass.builder()
                    .className(weaviateClassName)
                    .vectorizer("text2vec-transformers")
                    .vectorIndexType("hnsw")  // Стандартный индекс для векторов
                    .build();
            // Исправлено: Run() возвращает Result<Boolean>, а не Result<Object>
            Result<Boolean> createResult = weaviateClient.schema().classCreator()
                    .withClass(weaviateClass)  // Теперь принимает WeaviateClass
                    .run();
            if (createResult.hasErrors() && !createResult.getError().getMessages().toString().toLowerCase().contains("already exists")) {
                throw new RuntimeException("Ошибка создания класса: " + createResult.getError().getMessages());
            }
        } catch (Exception e) {
            throw new WeaviateException("Ошибка инициализации схемы Weaviate: " + e.getMessage(), e);
        }
    }
}
