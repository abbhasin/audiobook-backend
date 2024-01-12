package com.enigma.audiobook.backend.dao;

import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class InfluencerDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String INFLUENCER_REG_COLLECTION = "influencerReg";

    @Autowired
    public InfluencerDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }
}
