package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.ScoredContent;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertManyResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Sorts.ascending;

@Slf4j
@Repository
public class ScoredContentDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String SCORED_CONTENT_COLLECTION_FORMAT = "scoredContent_%s";

    public ScoredContentDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public void addBulkContentScores(String suffix, List<ScoredContent> scoredContents) {
        MongoCollection<Document> collection = getCollection(suffix);

        List<Document> docs = scoredContents.stream()
                .map(sc -> {
                    Document doc = Document.parse(serde.toJson(sc));
                    doc.append("postId", new ObjectId(doc.getString("postId")));
                    return doc;
                })
                .toList();
        try {
            InsertManyResult result = collection.insertMany(docs);
            // Prints the IDs of the inserted documents
            log.info("Inserted document ids: " + result.getInsertedIds());

            // Prints a message if any exceptions occur during the operation
        } catch (Exception e) {
            log.error("Unable to insert due to an error", e);
        }
    }

    public List<ScoredContent> getScoredContentSorted(String suffix, int limit) {
        MongoCollection<Document> collection = getCollection(suffix);

        FindIterable<Document> docs = collection.find()
                .sort(ascending("score","_id"))
//                .sort(ascending("_id"))
                .limit(limit);

        List<ScoredContent> scoreContent = new ArrayList<>();
        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                scoreContent.add(serde.fromJson(doc.toJson(), ScoredContent.class));
            }
        }

        return scoreContent;
    }

    public List<ScoredContent> getScoredContentSortedPaginated(String suffix, int limit,
                                                               ScoredContent lastScoredContent
    ) {
        MongoCollection<Document> collection = getCollection(suffix);

        Bson filterScoreGreater = Filters.gt("score", lastScoredContent.getScore());

        Bson filterScoreEqual = Filters.eq("score", lastScoredContent.getScore());
        Bson filterObjIdGreater = Filters.gt("_id", new ObjectId(lastScoredContent.getId()));
        Bson filterScoreAndObjId = Filters.and(filterScoreEqual, filterObjIdGreater);

        Bson finalFilter = Filters.or(filterScoreGreater, filterScoreAndObjId);

        FindIterable<Document> docs = collection.find(finalFilter)
                .sort(ascending("score","_id"))
                .limit(limit);

        List<ScoredContent> scoreContent = new ArrayList<>();
        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                scoreContent.add(serde.fromJson(doc.toJson(), ScoredContent.class));
            }
        }

        return scoreContent;
    }


    private MongoCollection<Document> getCollection(String suffix) {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(String.format(SCORED_CONTENT_COLLECTION_FORMAT, suffix));
    }
}
