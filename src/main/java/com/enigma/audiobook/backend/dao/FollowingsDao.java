package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.Following;
import com.enigma.audiobook.backend.models.FollowingType;
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

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.ascending;

@Slf4j
@Repository
public class FollowingsDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String FOLLOWINGS_COLLECTION = "followings";

    public FollowingsDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public void addFollowing(Following following) {
        MongoCollection<Document> collection = getCollection();

        try {
            Document doc = Document.parse(serde.toJson(following));
            doc
                    .append("_id", new ObjectId())
                    .append("followeeId", new ObjectId(doc.getString("followeeId")))
                    .append("followerUserId", new ObjectId(doc.getString("followerUserId")))
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime())
                    .append("isDeleted", false);
            InsertOneResult result = collection.insertOne(doc);

            log.info("Success! Inserted document id: " + result.getInsertedId());
        } catch (MongoException e) {
            log.error("Unable to insert into god registration", e);
            throw new RuntimeException(e);
        }
    }

    public void upsertFollowing(Following following) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document()
                .append("followeeId", new ObjectId(following.getFolloweeId()))
                .append("followingType", following.getFollowingType())
                .append("followerUserId", new ObjectId(following.getFollowerUserId()));

        Bson updates = Updates.combine(
                Updates.set("followeeId", new ObjectId(following.getFolloweeId())),
                Updates.set("followingType", following.getFollowingType()),
                Updates.set("followerUserId", new ObjectId(following.getFollowerUserId())),
                Updates.min("createTime", getCurrentTime()),
                Updates.set("updateTime", getCurrentTime()),
                Updates.set("isDeleted", false));

        UpdateOptions options = new UpdateOptions().upsert(true);
        try {

            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());
            if (result.getModifiedCount() <= 0 && result.getUpsertedId() == null) {
                throw new RuntimeException("unable to upsert");
            }
        } catch (MongoException e) {
            log.error("Unable to update due to an error", e);
            throw new RuntimeException(e);
        }
    }

    public void removeFollowing(Following following) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document()
                .append("followeeId", new ObjectId(following.getFolloweeId()))
                .append("followingType", following.getFollowingType())
                .append("followerUserId", new ObjectId(following.getFollowerUserId()));

        Bson updates =
                Updates.combine(
                        Updates.set("isDeleted", true),
                        Updates.set("updateTime", getCurrentTime()));

        UpdateOptions options = new UpdateOptions().upsert(false);
        try {

            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());
            if (result.getModifiedCount() <= 0 && result.getUpsertedId() == null) {
                throw new RuntimeException("unable to update");
            }
        } catch (MongoException e) {
            log.error("Unable to update due to an error", e);
            throw new RuntimeException(e);
        }
    }

    public List<Following> getFollowingsForUser(String userId) {
        MongoCollection<Document> collection = getCollection();

        FindIterable<Document> docs = collection.find(
                Filters.and(
                        eq("followerUserId", new ObjectId(userId)),
                        eq("isDeleted", false)
                )
        );

        List<Following> followings = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                followings.add(serde.fromJson(doc.toJson(), Following.class));
            }
        }

        log.info("followings for user:{} are:{}", userId, followings);

        return followings;
    }

    public int countFollowingsForFollowee(String followeeId, FollowingType followingType) {
        MongoCollection<Document> collection = getCollection();

        long count = collection.countDocuments(
                Filters.and(
                        eq("followeeId", new ObjectId(followeeId)),
                        eq("followingType", followingType.name()),
                        eq("isDeleted", false)
                )
        );

        return (int) count;
    }

    public List<Following> getFollowingsForUser(String userId, FollowingType followingType) {
        MongoCollection<Document> collection = getCollection();

        FindIterable<Document> docs = collection.find(
                Filters.and(
                        eq("followerUserId", new ObjectId(userId)),
                        eq("followingType", followingType.name()),
                        eq("isDeleted", false)
                )
        );

        List<Following> followings = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                followings.add(serde.fromJson(doc.toJson(), Following.class));
            }
        }

        return followings;
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(FOLLOWINGS_COLLECTION);
    }
}
