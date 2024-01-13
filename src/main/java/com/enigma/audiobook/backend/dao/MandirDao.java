package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.Influencer;
import com.enigma.audiobook.backend.models.Mandir;
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

    public void addMandir(Mandir mandir) {
        MongoCollection<Document> collection = getCollection();
        try {
            // Inserts a sample document describing a movie into the collection
            Document doc = Document.parse(gson.toJson(mandir)).append("_id", new ObjectId());
            InsertOneResult result = collection.insertOne(doc);
            // Prints the ID of the inserted document
            log.info("Success! Inserted document id: " + result.getInsertedId());

            // Prints a message if any exceptions occur during the operation
        } catch (MongoException e) {
            log.error("Unable to insert into user registration", e);
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
                mandirs.add(gson.fromJson(doc.toJson(), Mandir.class));
            }
        }

        return mandirs;
    }

    public List<Mandir> getMandirsPaginated(int limit, Mandir lastMandirObj) {
        MongoCollection<Document> collection = getCollection();
        Bson projectionFields = Projections.fields(
                Projections.include("_id", "imageUrl", "address"));
        FindIterable<Document> docs = collection.find(gt("_id", new ObjectId(lastMandirObj.getMandirId())))
                .projection(projectionFields)
                .sort(ascending("_id"))
                .limit(limit);

        List<Mandir> mandirs = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                mandirs.add(gson.fromJson(doc.toJson(), Mandir.class));
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
            return Optional.of(gson.fromJson(doc.toJson(), Mandir.class));
        }
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(MANDIR_REG_COLLECTION);
    }
}
