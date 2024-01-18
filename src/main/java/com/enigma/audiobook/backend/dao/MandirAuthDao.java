package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.MandirAuth;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.InsertOneResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
@Repository
public class MandirAuthDao extends BaseDao {
    private final MongoClient mongoClient;
    private final String database;
    private static final String MANDIR_AUTH_COLLECTION = "mandirAuth";

    public MandirAuthDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public void addMandirAuth(String mandirId, String userId, MandirAuth mandirAuth) {
        MongoCollection<Document> collection = getCollection();
        try {
            // Inserts a sample document describing a movie into the collection
            Document doc = new Document()
                    .append("_id", new ObjectId())
                    .append("mandirId", new ObjectId(mandirId))
                    .append("userId", new ObjectId(userId))
                    .append("mandirAuth", mandirAuth.toString())
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime())
                    .append("isDeleted", false);
            InsertOneResult result = collection.insertOne(doc);
            // Prints the ID of the inserted document
            log.info("Success! Inserted document id: " + result.getInsertedId());

            // Prints a message if any exceptions occur during the operation
        } catch (MongoException e) {
            log.error("Unable to insert into mandir auth registration", e);
            throw new RuntimeException(e);
        }
    }

    public List<String> getMandirsWithAdminPermission(String userId) {
        MongoCollection<Document> collection = getCollection();

        Bson projectionFields = Projections.fields(
                Projections.include("mandirId", "mandirAuth"));

        FindIterable<Document> docs = collection.find(eq("userId", new ObjectId(userId)))
                .projection(projectionFields);

        List<String> mandirs = new ArrayList<>();
        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                mandirs.add(doc.get("mandirId").toString());
            }
        }

        return mandirs;
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(MANDIR_AUTH_COLLECTION);
    }
}
