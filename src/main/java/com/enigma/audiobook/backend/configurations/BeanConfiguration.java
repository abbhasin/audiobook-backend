package com.enigma.audiobook.backend.configurations;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Singleton;

@Configuration
public class BeanConfiguration {

    @Bean
    @Singleton
    public MongoClient mongoClient(String uri) {
        return MongoClients.create(uri);
    }
}
