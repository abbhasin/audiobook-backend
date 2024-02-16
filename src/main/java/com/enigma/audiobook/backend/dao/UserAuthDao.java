package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.AuthAssociationType;
import com.enigma.audiobook.backend.models.AuthForUser;
import com.enigma.audiobook.backend.models.AuthType;
import com.enigma.audiobook.backend.models.MandirAuth;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
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
public class UserAuthDao extends BaseDao {
    private final MongoClient mongoClient;
    private final String database;
    private static final String USER_AUTH_COLLECTION = "userAuth";

    public UserAuthDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public void addUserAuth(String resourceId, String userId, AuthType authType, AuthAssociationType associationType) {
        MongoCollection<Document> collection = getCollection();
        try {
            // Inserts a sample document describing a movie into the collection
            Document doc = new Document()
                    .append("_id", new ObjectId())
                    .append("resourceId", new ObjectId(resourceId))
                    .append("userId", new ObjectId(userId))
                    .append("authType", authType.name())
                    .append("associationType", associationType.name())
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime())
                    .append("isDeleted", false);
            InsertOneResult result = collection.insertOne(doc);
            // Prints the ID of the inserted document
            log.info("Success! Inserted document id: " + result.getInsertedId());

            // Prints a message if any exceptions occur during the operation
        } catch (MongoException e) {
            log.error("Unable to insert into user auth registration", e);
            throw new RuntimeException(e);
        }
    }

    public List<AuthForUser> getAuthWithAdminPermission(String userId) {
        MongoCollection<Document> collection = getCollection();
        Bson filter = Filters.and(eq("userId", new ObjectId(userId)),
                eq("authType", AuthType.ADMIN.name()));

        FindIterable<Document> docs = collection.find(filter);

        List<AuthForUser> authsForUser = new ArrayList<>();
        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                authsForUser.add(serde.fromJson(doc.toJson(), AuthForUser.class));
            }
        }

        return authsForUser;
    }

    public boolean isUserAuthorized(String userId, String resourceId, AuthAssociationType associationType) {
        MongoCollection<Document> collection = getCollection();
        Bson filter = Filters.and(
                eq("userId", new ObjectId(userId)),
                eq("resourceId", new ObjectId(resourceId)),
                eq("associationType", associationType.name()),
                eq("authType", AuthType.ADMIN.name()));

        Document doc = collection.find(filter).first();
        return doc != null && !doc.isEmpty();
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(USER_AUTH_COLLECTION);
    }
}
