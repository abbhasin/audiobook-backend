package com.enigma.audiobook.backend.dao;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
@Repository
public class UserFeaturesDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String USER_FEAT_COLLECTION = "userFeatures";

    public UserFeaturesDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public void addOrUpdateUserFeature(String userId, UserFeature featureKey, boolean featureEnabledValue) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document().append("userId", new ObjectId(userId));

        Bson updates = Updates.combine(
                Updates.set("userId", userId),
                Updates.set(featureKey.name(), featureEnabledValue),
                Updates.set("updateTime", getCurrentTime())
        );

        UpdateOptions options = new UpdateOptions().upsert(true);
        try {

            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());
            if (result.getModifiedCount() <= 0 && result.getUpsertedId() == null) {
                throw new RuntimeException("unable to update user feature:" + featureKey);
            }
        } catch (MongoException e) {
            log.error("Unable to update due to an error", e);
            throw new RuntimeException(e);
        }
    }

    public boolean isFeatureEnabled(String userId, UserFeature featureKey) {
        MongoCollection<Document> collection = getCollection();
        Document doc = collection.find(eq("userId", new ObjectId(userId)))
                .first();

        if (doc == null) {
            return true;
        } else {
            return doc.getBoolean(featureKey.name(), false);
        }
    }

    public void initCollectionAndIndexes() {
        MongoDatabase db = mongoClient.getDatabase(database);
        db.createCollection(USER_FEAT_COLLECTION);

        MongoCollection<Document> collection = db.getCollection(USER_FEAT_COLLECTION);

        IndexOptions indexOptions = new IndexOptions()
                .name("userId_index")
                .unique(true);
        String resultCreateIndex = collection.createIndex(Indexes.descending("userId"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(USER_FEAT_COLLECTION);
    }

    public enum UserFeature {
        SWIPE_DARSHAN_PUG_ANIMATION
    }
}
