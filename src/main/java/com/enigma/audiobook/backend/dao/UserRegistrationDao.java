package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.User;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
@Repository
public class UserRegistrationDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String USER_REG_COLLECTION = "userRegistration";

    @Autowired
    public UserRegistrationDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public void registerNewUser(String userId) {
        MongoDatabase db = mongoClient.getDatabase(database);
        MongoCollection<Document> collection = db.getCollection(USER_REG_COLLECTION);
        try {
            // Inserts a sample document describing a movie into the collection
            InsertOneResult result = collection.insertOne(new Document()
                    .append("_id", new ObjectId())
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime())
                    .append("isDeleted", false));
            // Prints the ID of the inserted document
            log.info("Success! Inserted document id: " + result.getInsertedId());

            // Prints a message if any exceptions occur during the operation
        } catch (MongoException e) {
            log.error("Unable to insert into user registration", e);
            throw new RuntimeException(e);
        }
    }



    public void associateAuthenticatedUser(String userId, String authUserId, String phoneNumber) {
        MongoDatabase db = mongoClient.getDatabase(database);
        MongoCollection<Document> collection = db.getCollection("movies");
        Document query = new Document().append("_id",  new ObjectId(userId));

        Bson updates = Updates.combine(
                Updates.set("authUserId", authUserId),
                Updates.set("phoneNumber", phoneNumber),
                Updates.set("updateTime", getCurrentTime())
                );

        UpdateOptions options = new UpdateOptions().upsert(false);
        try {

            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());

            // Prints a message if any exceptions occur during the operation
        } catch (MongoException e) {
            log.error("Unable to update due to an error", e);
            throw new RuntimeException(e);
        }
    }

    public Optional<User> getUserWithAuthId(String authUserId) {
        MongoCollection<Document> collection = getCollection();
        // Creates instructions to project two document fields
        Bson projectionFields = Projections.fields(
                Projections.include("_id", "authUserId"));
        // Retrieves the first matching document, applying a projection and a descending sort to the results
        Document doc = collection.find(eq("authUserId", authUserId))
                .projection(projectionFields)
                .first();
        // Prints a message if there are no result documents, or prints the result document as JSON
        if (doc == null) {
            return Optional.empty();
        } else {
            return Optional.of(gson.fromJson(doc.toJson(), User.class));
        }
    }

    public Optional<User> getUser(String userId) {
        MongoCollection<Document> collection = getCollection();
        Document doc = collection.find(eq("_id", new ObjectId(userId)))
                .first();
        // Prints a message if there are no result documents, or prints the result document as JSON
        if (doc == null) {
            return Optional.empty();
        } else {
            return Optional.of(gson.fromJson(doc.toJson(), User.class));
        }
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(USER_REG_COLLECTION);
    }

}
