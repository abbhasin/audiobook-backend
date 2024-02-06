package com.enigma.audiobook.backend.dao;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
@Repository
public class CollectionConfigDao extends BaseDao {
    private final MongoClient mongoClient;
    private final String database;
    private static final String COLLECTION_CONFIG_COLLECTION = "collectonConfig";

    public CollectionConfigDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public void updateScoredContentCollectionName(String latestScoredCollectionName) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document().append("key", "latestScoredCollectionName");

        Bson updates = Updates.combine(
                Updates.set("key", "latestScoredCollectionName"),
                Updates.set("value", latestScoredCollectionName),
                Updates.set("updateTime", getCurrentTime())
        );

        UpdateOptions options = new UpdateOptions().upsert(true);
        try {

            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());
            if (result.getModifiedCount() <= 0 && result.getUpsertedId() == null) {
                throw new RuntimeException("unable to update");
            }
        } catch (MongoException e) {
            log.error("Unable to update due to an error", e);
            throw new RuntimeException(e);
        }
    }

    public Optional<String> getScoredCollectionName() {
        MongoCollection<Document> collection = getCollection();

        Document doc = collection.find(eq("key", "latestScoredCollectionName"))
                .first();
        if (doc == null) {
            return Optional.empty();
        } else {
            return Optional.of(doc.getString("value"));
        }
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(COLLECTION_CONFIG_COLLECTION);
    }
}
