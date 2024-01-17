package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.God;
import com.enigma.audiobook.backend.models.Influencer;
import com.mongodb.MongoException;
import com.mongodb.client.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Sorts.ascending;

@Slf4j
@Repository
public class InfluencerDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String INFLUENCER_REG_COLLECTION = "influencerReg";

    @Autowired
    public InfluencerDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public Influencer addInfluencer(Influencer influencer) {
        MongoCollection<Document> collection = getCollection();
        try {
            // Inserts a sample document describing a movie into the collection
            InsertOneResult result = collection.insertOne(Document.parse(serde.toJson(influencer))
                    .append("_id", new ObjectId())
                    .append("userId", new ObjectId(influencer.getUserId()))
                    .append("contentUploadStatus", ContentUploadStatus.PENDING)
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime())
                    .append("isDeleted", false));
            log.info("Success! Inserted document id: " + result.getInsertedId());

            return getInfleuncer(result.getInsertedId().asObjectId().getValue().toString()).get();
        } catch (MongoException e) {
            log.error("Unable to insert into user registration", e);
            throw new RuntimeException(e);
        }
    }

    public Influencer updateInfluencer(String userId, String imageUrl, ContentUploadStatus contentUploadStatus) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document().append("userId", new ObjectId(userId));

        Bson updates = Updates.combine(
                Updates.set("imageUrl", imageUrl),
                Updates.set("contentUploadStatus", contentUploadStatus.name()),
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

            return getInfleuncer(userId).get();
        } catch (Exception e) {
            log.error("Unable to update due to an error", e);
            throw new RuntimeException(e);
        }
    }

    public List<Influencer> getInfluencers(int limit) {
        MongoCollection<Document> collection = getCollection();
        Bson projectionFields = Projections.fields(
                Projections.include("_id", "userId", "imageUrl"));
        FindIterable<Document> docs = collection.find()
                .projection(projectionFields)
                .sort(ascending("_id"))
                .limit(limit);

        List<Influencer> influencers = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                influencers.add(serde.fromJson(doc.toJson(), Influencer.class));
            }
        }

        return influencers;
    }

    public List<Influencer> getInfleuncersPaginated(int limit, String lastInfluencerId) {
        MongoCollection<Document> collection = getCollection();
        Bson projectionFields = Projections.fields(
                Projections.include("_id", "userId", "imageUrl"));
        FindIterable<Document> docs = collection.find(gt("_id", new ObjectId(lastInfluencerId)))
                .projection(projectionFields)
                .sort(ascending("_id"))
                .limit(limit);

        List<Influencer> influencers = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                influencers.add(serde.fromJson(doc.toJson(), Influencer.class));
            }
        }

        return influencers;
    }

    public Optional<Influencer> getInfleuncer(String userId) {
        MongoCollection<Document> collection = getCollection();

        Document doc = collection.find(eq("userId", new ObjectId(userId)))
                .first();
        // Prints a message if there are no result documents, or prints the result document as JSON
        if (doc == null) {
            return Optional.empty();
        } else {
            return Optional.of(serde.fromJson(doc.toJson(), Influencer.class));
        }
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(INFLUENCER_REG_COLLECTION);
    }
}
