package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.God;
import com.enigma.audiobook.backend.models.Mandir;
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
public class MandirDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String MANDIR_REG_COLLECTION = "mandirReg";

    public MandirDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public Mandir initMandir(Mandir mandir) {
        MongoCollection<Document> collection = getCollection();
        try {
            // Inserts a sample document describing a movie into the collection
            Document doc = Document.parse(serde.toJson(mandir))
                    .append("_id", new ObjectId())
                    .append("imageUploadStatus", ContentUploadStatus.PENDING)
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime())
                    .append("isDeleted", false);
            InsertOneResult result = collection.insertOne(doc);
            // Prints the ID of the inserted document
            log.info("Success! Inserted document id: " + result.getInsertedId());

            return getMandir(result.getInsertedId().asObjectId().getValue().toString()).get();
        } catch (MongoException e) {
            log.error("Unable to insert into user registration", e);
            throw new RuntimeException(e);
        }
    }

    public Mandir updateMandir(String mandirId, String imageUrl, ContentUploadStatus imageUploadStatus) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document().append("_id", new ObjectId(mandirId));

        Bson updates = Updates.combine(
                Updates.set("imageUrl", imageUrl),
                Updates.set("imageUploadStatus", imageUploadStatus.name()),
                Updates.set("updateTime", getCurrentTime())
        );

        UpdateOptions options = new UpdateOptions().upsert(false);
        try {

            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());
            if (result.getModifiedCount() <= 0 || result.getUpsertedId() == null) {
                throw new RuntimeException("unable to update");
            }

            return getMandir(mandirId).get();
        } catch (MongoException e) {
            log.error("Unable to update due to an error", e);
            throw new RuntimeException(e);
        }
    }

    public List<Mandir> getMandirs(int limit) {
        MongoCollection<Document> collection = getCollection();
        Bson projectionFields = Projections.fields(
                Projections.include("_id", "imageUrl", "address"));
        FindIterable<Document> docs = collection.find()
                .projection(projectionFields)
                .sort(ascending("_id"))
                .limit(limit);

        List<Mandir> mandirs = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                mandirs.add(serde.fromJson(doc.toJson(), Mandir.class));
            }
        }

        return mandirs;
    }

    public List<Mandir> getMandirsPaginated(int limit, String lastMandirId) {
        MongoCollection<Document> collection = getCollection();
        Bson projectionFields = Projections.fields(
                Projections.include("_id", "imageUrl", "address"));
        FindIterable<Document> docs = collection.find(gt("_id", new ObjectId(lastMandirId)))
                .projection(projectionFields)
                .sort(ascending("_id"))
                .limit(limit);

        List<Mandir> mandirs = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                mandirs.add(serde.fromJson(doc.toJson(), Mandir.class));
            }
        }

        return mandirs;
    }

    public Optional<Mandir> getMandir(String mandirId) {
        MongoCollection<Document> collection = getCollection();

        Document doc = collection.find(eq("_id", new ObjectId(mandirId)))
                .first();
        // Prints a message if there are no result documents, or prints the result document as JSON
        if (doc == null) {
            return Optional.empty();
        } else {
            return Optional.of(serde.fromJson(doc.toJson(), Mandir.class));
        }
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(MANDIR_REG_COLLECTION);
    }
}
