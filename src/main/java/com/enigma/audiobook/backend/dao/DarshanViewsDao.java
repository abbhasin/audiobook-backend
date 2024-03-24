package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.DarshanView;
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

@Repository
@Slf4j
public class DarshanViewsDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String DARSHAN_VIEWS_COLLECTION = "darshanViews";

    public DarshanViewsDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public void upsertView(DarshanView darshanView) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document()
                .append("darshanId", new ObjectId(darshanView.getDarshanId()))
                .append("userId", new ObjectId(darshanView.getUserId()));

        Bson updates = Updates.combine(
                Updates.set("darshanId", new ObjectId(darshanView.getDarshanId())),
                Updates.set("userId", new ObjectId(darshanView.getUserId())),
                Updates.min("createTime", getCurrentTime()),
                Updates.set("updateTime", getCurrentTime()),
                Updates.max("viewDurationSec", darshanView.getViewDurationSec()),
                Updates.max("totalLengthSec", darshanView.getTotalLengthSec())
        );

        UpdateOptions options = new UpdateOptions().upsert(true);
        try {

            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());
            if (result.getModifiedCount() <= 0 && result.getUpsertedId() == null) {
                throw new RuntimeException("unable to upsert darshan view:" + darshanView);
            }
        } catch (MongoException e) {
            log.error("Unable to update due to an error", e);
            throw new RuntimeException(e);
        }
    }

    public void initCollectionAndIndexes() {
        MongoDatabase db = mongoClient.getDatabase(database);
        db.createCollection(DARSHAN_VIEWS_COLLECTION);

        MongoCollection<Document> collection = db.getCollection(DARSHAN_VIEWS_COLLECTION);

        IndexOptions indexOptions = new IndexOptions()
                .name("darshan_id_index");
        String resultCreateIndex = collection.createIndex(Indexes.descending("darshanId", "_id"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));

        indexOptions = new IndexOptions()
                .name("user_id_and_update_time_index");
        resultCreateIndex = collection.createIndex(Indexes.descending("userId", "updateTime", "_id"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(DARSHAN_VIEWS_COLLECTION);
    }
}
