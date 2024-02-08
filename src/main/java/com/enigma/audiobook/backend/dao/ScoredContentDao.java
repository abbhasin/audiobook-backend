package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.PostType;
import com.enigma.audiobook.backend.models.ScoredContent;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.*;

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

    public void addBulkScoredContent(String collectionName, List<ScoredContent> scoredContents) {
        MongoCollection<Document> collection = getCollectionByName(collectionName);

        List<Document> docs = scoredContents.stream()
                .map(sc -> {
                    Document doc = Document.parse(serde.toJson(sc));
                    doc.append("_id", new ObjectId());
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

    public void addScoredContent(String collectionName, ScoredContent scoredContent) {
        MongoCollection<Document> collection = getCollectionByName(collectionName);

        Document doc = Document.parse(serde.toJson(scoredContent));
        doc.append("_id", new ObjectId());
        doc.append("postId", new ObjectId(doc.getString("postId")));

        try {
            InsertOneResult result = collection.insertOne(doc);
            log.info("Inserted document id: " + result.getInsertedId());
        } catch (Exception e) {
            log.error("Unable to insert due to an error", e);
        }
    }

    public List<ScoredContent> getScoredContentSorted(String collectionName, int limit, PostType postType) {
        MongoCollection<Document> collection = getCollectionByName(collectionName);

        FindIterable<Document> docs = collection.find(eq("postType", postType.name()))
                .sort(descending("score", "postId"))
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

    public List<ScoredContent> getScoredContentSortedPaginated(String collectionName, int limit,
                                                               PostType postType,
                                                               ScoredContent lastScoredContent
    ) {
        MongoCollection<Document> collection = getCollectionByName(collectionName);

        Bson filterScoreLesser = Filters.lt("score", lastScoredContent.getScore());

        Bson filterScoreEqual = eq("score", lastScoredContent.getScore());
        Bson filterPostIdLesser = Filters.lt("postId", new ObjectId(lastScoredContent.getPostId()));
        Bson filterScoreAndPostId = Filters.and(filterScoreEqual, filterPostIdLesser);

        Bson finalFilter = Filters.and(eq("postType", postType.name()),
                Filters.or(filterScoreLesser, filterScoreAndPostId));

//        Bson orderBySort = orderBy(descending("score"), ascending("_id"));
        FindIterable<Document> docs = collection.find(finalFilter)
                .sort(descending("score", "postId"))
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

    public void initCollectionAndIndexes(String collectionName) {
        MongoDatabase db = mongoClient.getDatabase(database);
        db.createCollection(collectionName);

        MongoCollection<Document> collection = db.getCollection(collectionName);

        IndexOptions indexOptions = new IndexOptions()
                .name("score_and_postId_descending_index")
                .unique(true);
        String resultCreateIndex = collection.createIndex(Indexes.descending("score", "postId"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));
    }

    private MongoCollection<Document> getCollectionByName(String collectionName) {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(collectionName);
    }

    public static String getCollectionName(String suffix) {
        return String.format(SCORED_CONTENT_COLLECTION_FORMAT, suffix);
    }
}
