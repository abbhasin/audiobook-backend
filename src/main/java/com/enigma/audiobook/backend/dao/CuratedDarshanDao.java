package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.CuratedDarshan;
import com.enigma.audiobook.backend.models.Influencer;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.InsertOneResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;

@Slf4j
@Repository
public class CuratedDarshanDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String CURATED_DARSHAN_COLLECTION = "curatedDarshans";

    public CuratedDarshanDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public CuratedDarshan addCuratedDarshan(CuratedDarshan curatedDarshan) {
        MongoCollection<Document> collection = getCollection();
        try {
            // Inserts a sample document describing a movie into the collection
            InsertOneResult result = collection.insertOne(Document.parse(serde.toJson(curatedDarshan))
                    .append("_id", new ObjectId())
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime())
                    .append("isDeleted", false));
            log.info("Success! Inserted document id: " + result.getInsertedId());

            return getCuratedDarshan(result.getInsertedId().asObjectId().getValue().toString()).get();
        } catch (MongoException e) {
            log.error("Unable to insert", e);
            throw new RuntimeException(e);
        }
    }

    public List<CuratedDarshan> getLastNCuratedDarshan(int n) {
        MongoCollection<Document> collection = getCollection();
        FindIterable<Document> docs = collection.find()
                .sort(descending("_id"))
                .limit(n);

        List<CuratedDarshan> curatedDarshans = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                curatedDarshans.add(serde.fromJson(doc.toJson(), CuratedDarshan.class));
            }
        }

        return curatedDarshans;
    }

    public Optional<CuratedDarshan> getCuratedDarshan(String curatedDarshanId) {
        MongoCollection<Document> collection = getCollection();

        Document doc = collection.find(eq("_id", new ObjectId(curatedDarshanId)))
                .first();
        // Prints a message if there are no result documents, or prints the result document as JSON
        if (doc == null) {
            return Optional.empty();
        } else {
            return Optional.of(serde.fromJson(doc.toJson(), CuratedDarshan.class));
        }
    }

    public void initCollectionAndIndexes() {
        MongoDatabase db = mongoClient.getDatabase(database);
        db.createCollection(CURATED_DARSHAN_COLLECTION);

        MongoCollection<Document> collection = db.getCollection(CURATED_DARSHAN_COLLECTION);
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(CURATED_DARSHAN_COLLECTION);
    }
}
