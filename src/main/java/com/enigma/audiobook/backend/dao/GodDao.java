package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.God;
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
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Sorts.ascending;

@Slf4j
@Repository
public class GodDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String GOD_REG_COLLECTION = "godReg";

    public GodDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public God addGod(God god) {
        MongoCollection<Document> collection = getCollection();
        log.info("zzz god:" + serde.toJson(god));
        log.info("zzz doc:" + Document.parse(serde.toJson(god)));
        try {
            Document doc = Document.parse(serde.toJson(god))
                    .append("_id", new ObjectId())
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime())
                    .append("isDeleted", false)
                    .append("contentUploadStatus", ContentUploadStatus.PENDING);
            InsertOneResult result = collection.insertOne(doc);

            log.info("Success! Inserted document id: " + result.getInsertedId());
            return getGod(result.getInsertedId().asObjectId().getValue().toString()).get();
        } catch (MongoException e) {
            log.error("Unable to insert into god registration", e);
            throw new RuntimeException(e);
        }
    }

    public God updateGod(String godId, String imageUrl, ContentUploadStatus contentUploadStatus) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document().append("_id", new ObjectId(godId));

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

            return getGod(godId).get();
        } catch (MongoException e) {
            log.error("Unable to update due to an error", e);
            throw new RuntimeException(e);
        }
    }

    public List<God> getGods(int limit) {
        MongoCollection<Document> collection = getCollection();
        Bson projectionFields = Projections.fields(
                Projections.include("_id", "godName", "imageUrl"));
        FindIterable<Document> docs = collection.find()
                .projection(projectionFields)
                .sort(ascending("_id"))
                .limit(limit);

        List<God> gods = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                gods.add(serde.fromJson(doc.toJson(), God.class));
            }
        }

        return gods;
    }

    public List<God> getGodsPaginated(int limit, String lastGodId) {
        MongoCollection<Document> collection = getCollection();
        Bson projectionFields = Projections.fields(
                Projections.include("_id", "godName", "imageUrl"));
        FindIterable<Document> docs = collection.find(gt("_id", new ObjectId(lastGodId)))
                .projection(projectionFields)
                .sort(ascending("_id"))
                .limit(limit);

        List<God> gods = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                gods.add(serde.fromJson(doc.toJson(), God.class));
            }
        }

        return gods;
    }

    public Optional<God> getGod(String godId) {
        MongoCollection<Document> collection = getCollection();

        Document doc = collection.find(eq("_id", new ObjectId(godId)))
                .first();
        // Prints a message if there are no result documents, or prints the result document as JSON
        if (doc == null) {
            return Optional.empty();
        } else {
            return Optional.of(serde.fromJson(doc.toJson(), God.class));
        }
    }


    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(GOD_REG_COLLECTION);
    }
}
