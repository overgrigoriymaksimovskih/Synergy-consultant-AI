package com.synergyconsultant.configuration;

import io.weaviate.client.WeaviateClient;
import io.weaviate.client.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeaviateConfig {

    @Value("${weaviate.url}")
    private String weaviateUrl;

    @Value("${weaviate.apiKey}")
    private String apiKey;

    @Bean
    public WeaviateClient weaviateClient() {
        Config config = new Config(weaviateUrl, apiKey);
        return new WeaviateClient(config);
    }
}

