package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.User;
import com.enigma.audiobook.backend.proxies.FirebaseClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public User registerNewUser(Map<String, String> initUserMetadata) {
        MongoCollection<Document> collection = getCollection();
        try {
            InsertOneResult result = collection.insertOne(new Document()
                    .append("_id", new ObjectId())
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime())
                    .append("initUserMetadata", initUserMetadata)
                    .append("isDeleted", false));

            log.info("Success! Inserted document id: " + result.getInsertedId());

            return getUser(result.getInsertedId().asObjectId().getValue().toString()).get();
        } catch (MongoException e) {
            log.error("Unable to insert into user registration", e);
            throw new RuntimeException(e);
        }
    }


    public void associateAuthenticatedUser(String userId, String authUserId, String phoneNumber, Map<String, String> associateUserMetadata) {

        User user = getUser(userId).get();

        User.Metadata metadata = user.getMetadata() != null ? user.getMetadata() : new User.Metadata();
        List<String> otherAuths = metadata.getOtherAuthUserIds() != null ? metadata.getOtherAuthUserIds() : new ArrayList<>();
        otherAuths.add(authUserId);
        metadata.setOtherAuthUserIds(otherAuths);

        List<String> userDetails = metadata.getUserDetails() != null ? metadata.getUserDetails() : new ArrayList<>();
        userDetails.add(phoneNumber);
        metadata.setUserDetails(userDetails);

        MongoCollection<Document> collection = getCollection();
        Document query = new Document().append("_id", new ObjectId(userId));

        Document metadataDoc = Document.parse(serde.toJson(metadata));
        Bson updates = Updates.combine(
                Updates.set("authUserId", authUserId),
                Updates.set("phoneNumber", phoneNumber),
                Updates.set("updateTime", getCurrentTime()),
                Updates.set("associateUserMetadata", associateUserMetadata),
                Updates.set("metadata", metadataDoc)
        );

        UpdateOptions options = new UpdateOptions().upsert(false);
        try {

            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());
            if (result.getModifiedCount() <= 0) {
                throw new RuntimeException("unable to associate auth user with a user");
            }

            // Prints a message if any exceptions occur during the operation
        } catch (MongoException e) {
            log.error("Unable to update due to an error", e);
            throw new RuntimeException(e);
        }
    }

    public void updateMetadata(String userId, String unAssociatedAuthUserId,
                               FirebaseClient.FirebaseUserInfo userInfo, Map<String, String> associateUserMetadata) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document().append("_id", new ObjectId(userId));

        User user = getUser(userId).get();

        User.Metadata metadata = user.getMetadata() != null ? user.getMetadata() : new User.Metadata();
        List<String> otherAuths = metadata.getOtherAuthUserIds() != null ? metadata.getOtherAuthUserIds() : new ArrayList<>();
        otherAuths.add(unAssociatedAuthUserId);
        metadata.setOtherAuthUserIds(otherAuths);

        List<String> userDetails = metadata.getUserDetails() != null ? metadata.getUserDetails() : new ArrayList<>();
        userDetails.add(userInfo.getPhoneNum());
        metadata.setUserDetails(userDetails);

        Document metadataDoc = Document.parse(serde.toJson(metadata));
        log.info("metadata doc:{}", metadataDoc);
        Bson updates = Updates.combine(
                Updates.set("metadata", metadataDoc),
                Updates.set("associateUserMetadata", associateUserMetadata),
                Updates.set("updateTime", getCurrentTime())
        );

        UpdateOptions options = new UpdateOptions().upsert(false);
        try {

            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());
            if (result.getModifiedCount() <= 0) {
                throw new RuntimeException("unable to associate auth user with a user");
            }

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
            return Optional.of(serde.fromJson(doc.toJson(), User.class));
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
            return Optional.of(serde.fromJson(doc.toJson(), User.class));
        }
    }

    public void initCollectionAndIndexes() {
        MongoDatabase db = mongoClient.getDatabase(database);
        db.createCollection(USER_REG_COLLECTION);

        MongoCollection<Document> collection = db.getCollection(USER_REG_COLLECTION);

        IndexOptions indexOptions = new IndexOptions()
                .name("auth_id_index");
        String resultCreateIndex = collection.createIndex(Indexes.ascending("authUserId"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));

        indexOptions = new IndexOptions()
                .name("phone_num_index");
        resultCreateIndex = collection.createIndex(Indexes.ascending("phoneNumber"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(USER_REG_COLLECTION);
    }

}
