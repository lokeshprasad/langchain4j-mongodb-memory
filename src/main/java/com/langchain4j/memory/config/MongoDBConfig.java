package com.langchain4j.memory.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoDBConfig {

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(System.getenv("MONGODB_URI")); // Get MongoDB URI from environment
    }
}