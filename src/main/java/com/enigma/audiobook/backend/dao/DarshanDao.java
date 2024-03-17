package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.Darshan;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
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
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;

@Slf4j
@Repository
public class DarshanDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    public static final String DARSHAN_REG_COLLECTION = "DarshanReg";

    public DarshanDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public String generateId() {
        return new ObjectId().toString();
    }

    public Darshan initDarshan(Darshan darshan, String darshanId) {
        MongoCollection<Document> collection = getCollection();
        try {
            ObjectId id = new ObjectId(darshanId);

            darshan.setVideoUploadStatus(ContentUploadStatus.PENDING);

            // Inserts a sample document describing a movie into the collection
            Document doc = Document.parse(serde.toJson(darshan))
                    .append("_id", id)
                    .append("mandirId", new ObjectId(darshan.getMandirId()))
                    .append("godId", new ObjectId(darshan.getGodId()))
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime());
            InsertOneResult result = collection.insertOne(doc);
            // Prints the ID of the inserted document
            log.info("Success! Inserted document id: " + result.getInsertedId());
            return getDarshan(result.getInsertedId().asObjectId().getValue().toString()).get();
        } catch (MongoException e) {
            log.error("Unable to insert into god registration", e);
            throw new RuntimeException(e);
        }
    }

    public Darshan updateDarshanStatus(String darshanId, ContentUploadStatus status) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document().append("_id", new ObjectId(darshanId));
        Bson updates = Updates.combine(
                Updates.set("videoUploadStatus", status.name()),
                Updates.set("updateTime", getCurrentTime())
        );

        UpdateOptions options = new UpdateOptions().upsert(false);

        try {
            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());

            if (result.getModifiedCount() <= 0) {
                throw new IllegalStateException("unable to update darshan:" + darshanId);
            }

            return getDarshan(darshanId).get();
        } catch (MongoException e) {
            log.error("Unable to update due to an error: ", e);
            throw new RuntimeException(e);
        }
    }

    public Darshan updateDarshan(String darshanId, String thumbnailUrl, String videoUrl, ContentUploadStatus status) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document().append("_id", new ObjectId(darshanId));
        Bson updates = Updates.combine(
                Updates.set("videoUploadStatus", status.name()),
                Updates.set("thumbnailUrl", thumbnailUrl),
                Updates.set("videoUrl", videoUrl),
                Updates.set("updateTime", getCurrentTime())
        );
        UpdateOptions options = new UpdateOptions().upsert(false);

        try {
            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());

            if (result.getModifiedCount() <= 0) {
                throw new IllegalStateException("unable to update darshan:" + darshanId);
            }

            return getDarshan(darshanId).get();
        } catch (MongoException e) {
            log.error("Unable to update due to an error: ", e);
            throw new RuntimeException(e);
        }
    }

    public Optional<Darshan> getDarshan(String darshanId) {
        MongoCollection<Document> collection = getCollection();

        Document doc = collection.find(eq("_id", new ObjectId(darshanId)))
                .first();
        // Prints a message if there are no result documents, or prints the result document as JSON
        if (doc == null) {
            return Optional.empty();
        } else {
            return Optional.of(serde.fromJson(doc.toJson(), Darshan.class));
        }
    }

    public List<Darshan> getDarshans(String mandirId, String godId, ContentUploadStatus status) {
        MongoCollection<Document> collection = getCollection();
        Bson filter = Filters.and(
                Filters.eq("mandirId", new ObjectId(mandirId)),
                Filters.eq("godId", new ObjectId(godId)),
                Filters.eq("videoUploadStatus", status.name()));

        FindIterable<Document> docs = collection.find(filter)
                .sort(descending("createTime"))
                .limit(10);

        List<Darshan> darshans = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                darshans.add(serde.fromJson(doc.toJson(), Darshan.class));
            }
        }

        return darshans;
    }

    public List<Darshan> getDarshanByGod(String godId, ContentUploadStatus status,
                                         Optional<String> lastDarshanIdOpt,
                                         Optional<String> excludeMandirIdOpt,
                                         int limit) {
        MongoCollection<Document> collection = getCollection();
        Bson filter = Filters.and(
                Filters.eq("godId", new ObjectId(godId)),
                Filters.eq("videoUploadStatus", status.name()));

        if (excludeMandirIdOpt.isPresent()) {
            filter = Filters.and(Filters.ne("mandirId", new ObjectId(excludeMandirIdOpt.get())),
                    filter);
        }

        if (lastDarshanIdOpt.isPresent()) {
            filter = Filters.and(Filters.gt("_id", new ObjectId(lastDarshanIdOpt.get())),
                    filter);
        }

        FindIterable<Document> docs = collection.find(filter)
                .sort(ascending("_id"))
                .limit(limit);

        List<Darshan> darshans = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                darshans.add(serde.fromJson(doc.toJson(), Darshan.class));
            }
        }

        return darshans;
    }

    public List<Darshan> getDarshanByStatus(ContentUploadStatus status,
                                            int limit) {
        MongoCollection<Document> collection = getCollection();
        Bson filter = Filters.eq("videoUploadStatus", status.name());

        FindIterable<Document> docs = collection.find(filter)
                .sort(ascending("_id"))
                .limit(limit);

        List<Darshan> darshans = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                darshans.add(serde.fromJson(doc.toJson(), Darshan.class));
            }
        }

        return darshans;
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(DARSHAN_REG_COLLECTION);
    }
}
