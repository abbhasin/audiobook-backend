package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.NewPost;
import com.enigma.audiobook.backend.models.PostType;
import com.enigma.audiobook.backend.models.ScoredContent;
import com.enigma.audiobook.backend.models.View;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
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
public class NewPostsDao extends BaseDao {

    private final MongoClient mongoClient;
    private final String database;
    private static final String NEW_POSTS_COLLECTION = "newPosts";

    public NewPostsDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public void upsertNewPost(NewPost newPost) {
        MongoCollection<Document> collection = getCollection();

        Document query = new Document()
                .append("postId", new ObjectId(newPost.getPostId()))
                .append("postType", newPost.getPostType().name());

        Bson updates = Updates.combine(
                Updates.set("postId", new ObjectId(newPost.getPostId())),
                Updates.set("postType", newPost.getPostType().name()),
                Updates.set("updateTime", getCurrentTime())
        );

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

    public void removeFromNewPosts(String postId) {
        MongoCollection<Document> collection = getCollection();
        Bson query = eq("postId", new ObjectId(postId));
        try {

            DeleteResult result = collection.deleteOne(query);
            log.info("Deleted document count: " + result.getDeletedCount());
        } catch (MongoException e) {
            log.error("Unable to delete due to an error: ", e);
            throw new RuntimeException(e);
        }
    }

    public List<NewPost> getNewPostsByTypeNext(PostType postType, int limit, Optional<NewPost> lastNewPost) {
        MongoCollection<Document> collection = getCollection();
        Bson finalFilter = Filters.eq("postType", postType.name());

        if (lastNewPost.isPresent()) {
            Bson filterPostIdLesser = Filters.lt("postId", new ObjectId(lastNewPost.get().getPostId()));
            finalFilter = Filters.and(
                    finalFilter,
                    filterPostIdLesser);
        }

        FindIterable<Document> docs = collection.find(finalFilter)
                .sort(descending("postId"))
                .limit(limit);

        List<NewPost> views = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                views.add(serde.fromJson(doc.toJson(), NewPost.class));
            }
        }

        return views;
    }

    public void initCollectionAndIndexes() {
        MongoDatabase db = mongoClient.getDatabase(database);
        db.createCollection(NEW_POSTS_COLLECTION);

        MongoCollection<Document> collection = db.getCollection(NEW_POSTS_COLLECTION);

        IndexOptions indexOptions = new IndexOptions()
                .name("post_type_and_id_index");
        String resultCreateIndex = collection.createIndex(Indexes.descending("postType", "postId"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(NEW_POSTS_COLLECTION);
    }
}
