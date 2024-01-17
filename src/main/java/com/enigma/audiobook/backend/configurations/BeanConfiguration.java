package com.enigma.audiobook.backend.configurations;

import com.enigma.audiobook.backend.dao.UserRegistrationDao;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.enigma.audiobook.backend")
public class BeanConfiguration {
    static final String MONGO_LOCAL_URL = "mongodb://127.0.0.1:27017/?directConnection=true&serverSelectionTimeoutMS=2000&appName=service";
    static final String DATABASE = "dev";

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(MONGO_LOCAL_URL);
    }

    @Bean
    public String database() {
        return DATABASE;
    }

    @Bean
    public UserRegistrationDao userRegistrationDao(MongoClient mongoClient) {
        return new UserRegistrationDao(mongoClient, DATABASE);
    }
}
