package com.enigma.audiobook.backend.dao;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class DarshanDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String DARSHAN_REG_COLLECTION = "DarshanReg";

    public DarshanDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(DARSHAN_REG_COLLECTION);
    }
}
