package com.enigma.audiobook.backend.configurations;

import com.enigma.audiobook.backend.dao.*;
import com.enigma.audiobook.backend.models.MandirAuth;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableScheduling
@ComponentScan(basePackages = "com.enigma.audiobook.backend")
public class BeanConfiguration {
    static final String MONGO_LOCAL_URL = "mongodb://127.0.0.1:27017/?directConnection=true&serverSelectionTimeoutMS=2000&appName=service";
    static final String DATABASE = "dev";

    @Bean
    public MongoClient mongoClient(@Value("${mongo.url}") String url) {
        return MongoClients.create(url);
    }

    @Bean
    public UserRegistrationDao userRegistrationDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new UserRegistrationDao(mongoClient, database);
    }

    @Bean
    public GodDao godDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new GodDao(mongoClient, database);
    }

    @Bean
    public MandirDao mandirDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new MandirDao(mongoClient, database);
    }

    @Bean
    public InfluencerDao influencerDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new InfluencerDao(mongoClient, database);
    }

    @Bean(name = "justDarshanDao")
    public DarshanDao darshanDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new DarshanDao(mongoClient, database);
    }

    @Bean
    public FollowingsDao followingsDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new FollowingsDao(mongoClient, database);
    }

    @Bean
    public PostsDao postsDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new PostsDao(mongoClient, database);
    }

    @Bean
    public ScoredContentDao scoredContentDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new ScoredContentDao(mongoClient, database);
    }

    @Bean
    public UserAuthDao userAuthDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new UserAuthDao(mongoClient, database);
    }

    @Bean
    public NewPostsDao newPostsDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new NewPostsDao(mongoClient, database);
    }

    @Bean
    public ViewsDao viewsDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new ViewsDao(mongoClient, database);
    }

    @Bean
    public CuratedDarshanDao curatedDarshanDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new CuratedDarshanDao(mongoClient, database);
    }

    @Bean
    public CollectionConfigDao collectionConfigDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new CollectionConfigDao(mongoClient, database);
    }

    @Bean
    public MandirAuthDao mandirAuthDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new MandirAuthDao(mongoClient, database);
    }

    @Bean
    public UserFeaturesDao userFeaturesDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new UserFeaturesDao(mongoClient, database);
    }

    @Bean
    public DarshanViewsDao darshanViewsDao(MongoClient mongoClient, @Value("${mongo.database}") String database) {
        return new DarshanViewsDao(mongoClient, database);
    }

    @Bean
    @Qualifier(value = "appJobsScheduler")
    public ScheduledExecutorService appJobsScheduler() {
        return Executors.newScheduledThreadPool(5);
    }
}
