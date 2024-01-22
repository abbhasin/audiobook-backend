package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.View;
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
public class ViewsDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String VIEWS_COLLECTION = "views";

    public ViewsDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public void addView(View view) {
        MongoCollection<Document> collection = getCollection();

        try {
            Document doc = Document.parse(serde.toJson(view));
            doc.append("_id", new ObjectId())
                    .append("postId", new ObjectId(doc.getString("postId")))
                    .append("userId", new ObjectId(doc.getString("userId")))
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime())
                    .append("isDeleted", false);
            InsertOneResult result = collection.insertOne(doc);

            log.info("Success! Inserted document id: " + result.getInsertedId());
        } catch (MongoException e) {
            log.error("Unable to insert", e);
            throw new RuntimeException(e);
        }
    }

    public void upsertView(View view) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document()
                .append("postId", new ObjectId(view.getPostId()))
                .append("userId", new ObjectId(view.getUserId()));

        Bson updates = Updates.combine(
                Updates.set("postId", new ObjectId(view.getPostId())),
                Updates.set("userId", new ObjectId(view.getUserId())),
                Updates.min("createTime", getCurrentTime()),
                Updates.set("updateTime", getCurrentTime()),
                Updates.max("viewDurationSec", view.getViewDurationSec()),
                Updates.max("totalLengthSec", view.getTotalLengthSec())
        );

        UpdateOptions options = new UpdateOptions().upsert(true);
        try {

            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());
            if (result.getModifiedCount() <= 0 && result.getUpsertedId() == null) {
                throw new RuntimeException("unable to associate auth user with a user");
            }
        } catch (MongoException e) {
            log.error("Unable to update due to an error", e);
            throw new RuntimeException(e);
        }
    }

    public List<View> getViewsForUser(String userId, int limit) {
        MongoCollection<Document> collection = getCollection();

        // from latest to last
        FindIterable<Document> docs = collection.find(eq("userId", new ObjectId(userId)))
                .sort(descending("updateTime", "_id"))
                .limit(limit);

        List<View> views = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                views.add(serde.fromJson(doc.toJson(), View.class));
            }
        }

        return views;
    }

    public List<View> getViewsForUserNext(String userId, int limit, View lastView) {
        MongoCollection<Document> collection = getCollection();
        // dont need a complex or criteria like the scored content since updateTime for single user would mostly
        // be unique
        Bson filterUpdateTimeLesser = Filters.lt("updateTime", lastView.getUpdateTime());
        Bson finalFilter = Filters.and(
                eq("userId", new ObjectId(userId)),
                filterUpdateTimeLesser);
        FindIterable<Document> docs = collection.find(finalFilter)
                .sort(descending("updateTime", "_id"))
                .limit(limit);

        List<View> views = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                views.add(serde.fromJson(doc.toJson(), View.class));
            }
        }

        return views;
    }

    public List<View> getViewsForPost(String postId, int limit) {
        MongoCollection<Document> collection = getCollection();

        FindIterable<Document> docs = collection.find(eq("postId", new ObjectId(postId)))
                .sort(descending("_id"))
                .limit(limit);

        List<View> views = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                views.add(serde.fromJson(doc.toJson(), View.class));
            }
        }

        return views;
    }

    public List<View> getViewsForPostNext(String postId, int limit, Optional<String> lastViewId) {
        MongoCollection<Document> collection = getCollection();
        Bson finalFilter = Filters.eq("postId", new ObjectId(postId));

        if (lastViewId.isPresent()) {
            Bson filterIdLesser = Filters.lt("_id", lastViewId.get());
            finalFilter = Filters.and(
                    finalFilter,
                    filterIdLesser);
        }

        FindIterable<Document> docs = collection.find(finalFilter)
                .sort(descending("_id"))
                .limit(limit);

        List<View> views = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                views.add(serde.fromJson(doc.toJson(), View.class));
            }
        }

        return views;
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(VIEWS_COLLECTION);
    }
}
