package com.enigma.audiobook.backend.dao;

import com.enigma.audiobook.backend.models.*;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;

@Slf4j
@Repository
public class PostsDao extends BaseDao {
    private final MongoClient mongoClient;
    private final String database;
    public static final String POSTS_COLLECTION = "Posts";

    public PostsDao(MongoClient mongoClient, String database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public String generateId() {
        ObjectId id = new ObjectId();
        return id.toString();
    }

    public Post initPost(Post post, String postId, Map<String, String> initMetadata) {
        MongoCollection<Document> collection = getCollection();
        try {
            ObjectId id = new ObjectId(postId);

            if (post.getType() != PostType.TEXT) {
                post.setContentUploadStatus(ContentUploadStatus.PENDING);
            } else {
                post.setContentUploadStatus(ContentUploadStatus.SUCCESS_NO_CONTENT);
            }

            // Inserts a sample document describing a movie into the collection
            Document doc = Document.parse(serde.toJson(post))
                    .append("_id", id)
                    .append("createTime", getCurrentTime())
                    .append("updateTime", getCurrentTime())
                    .append("initMetadata", initMetadata)
                    .append("isDeleted", false);
            switch (post.getAssociationType()) {
                case MANDIR:
                    doc.append("associatedMandirId", new ObjectId(post.getAssociatedMandirId()));
                    doc.remove("associatedInfluencerId");
                    doc.remove("associatedGodId");
                    break;
                case INFLUENCER:
                    doc.append("associatedInfluencerId", new ObjectId(post.getAssociatedInfluencerId()));
                    doc.remove("associatedMandirId");
                    doc.remove("associatedGodId");
                    break;
                case GOD:
                    doc.append("associatedGodId", new ObjectId(post.getAssociatedGodId()));
                    doc.remove("associatedInfluencerId");
                    doc.remove("associatedMandirId");
                    break;
                default:
                    throw new IllegalStateException("unhandled post association type:" + post.getAssociationType());
            }
            doc.append("fromUserId", new ObjectId(post.getFromUserId()));

            InsertOneResult result = collection.insertOne(doc);
            // Prints the ID of the inserted document
            log.info("Success! Inserted document id: " + result.getInsertedId());
            post.setPostId(result.getInsertedId().asObjectId().getValue().toString());
            return post;
        } catch (MongoException e) {
            log.error("Unable to insert", e);
            throw new RuntimeException(e);
        }
    }

    public Post updatePostStatus(String postId, ContentUploadStatus status, Map<String, String> completeUploadMetadata) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document().append("_id", new ObjectId(postId));

        Bson updates =
                Updates.combine(
                        Updates.set("contentUploadStatus", status.name()),
                        Updates.set("completeUploadMetadata", completeUploadMetadata),
                        Updates.set("updateTime", getCurrentTime()));
        UpdateOptions options = new UpdateOptions().upsert(false);

        try {
            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());

            if (result.getModifiedCount() <= 0) {
                throw new IllegalStateException("unable to modify post for id:" + postId);
            }

            return getPost(postId).get();
        } catch (MongoException e) {
            log.error("Unable to update due to an error: ", e);
            throw new RuntimeException(e);
        }
    }

    public Post updatePost(String postId, ContentUploadStatus status, PostType postType, String thumbnailUrl,
                           String videoUrl, List<String> imagesUrls, String audioUrl) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document().append("_id", new ObjectId(postId));

        Bson statusUpdate = Updates.combine(
                Updates.set("contentUploadStatus", status.name()),
                Updates.set("updateHLSTime", getCurrentTime()));
        Bson updates;
        switch (postType) {
            case VIDEO:
                updates = Updates.combine(
                        statusUpdate,
                        Updates.set("thumbnailUrl", thumbnailUrl),
                        Updates.set("videoUrl", videoUrl)
                );
                break;
            case AUDIO:
                updates = Updates.combine(
                        statusUpdate,
                        Updates.set("audioUrl", audioUrl));
                break;
            case IMAGES:
                updates = Updates.combine(
                        statusUpdate,
                        Updates.set("imagesUrl", imagesUrls));
                break;
            case TEXT:
                updates = statusUpdate;
                break;
            default:
                throw new IllegalStateException("unhandled postType:" + postType);
        }

        UpdateOptions options = new UpdateOptions().upsert(false);

        try {
            UpdateResult result = collection.updateOne(query, updates, options);

            log.info("Modified document count: " + result.getModifiedCount());
            log.info("Upserted id: " + result.getUpsertedId());

            if (result.getModifiedCount() <= 0) {
                throw new IllegalStateException("unable to modify post for id:" + postId);
            }

            return getPost(postId).get();
        } catch (MongoException e) {
            log.error("Unable to update due to an error: ", e);
            throw new RuntimeException(e);
        }
    }

    public Optional<Post> getPost(String postId) {
        MongoCollection<Document> collection = getCollection();

        Document doc = collection.find(eq("_id", new ObjectId(postId)))
                .first();
        // Prints a message if there are no result documents, or prints the result document as JSON
        if (doc == null) {
            return Optional.empty();
        } else {
            return Optional.of(serde.fromJson(doc.toJson(), Post.class));
        }
    }

    public List<Post> getPostForInfluencer(String influencerId, int limit,
                                           Optional<String> lastPostId,
                                           boolean onlyProcessed) {
        MongoCollection<Document> collection = getCollection();

        Bson fromUserIdFilter = eq("fromUserId", new ObjectId(influencerId));
        Bson influencerIdFilter = eq("associatedInfluencerId", new ObjectId(influencerId));
        Bson filter = Filters.and(Filters.eq("isDeleted", false), Filters.or(fromUserIdFilter, influencerIdFilter));

        Bson contentFilter = Filters.or(
                Filters.eq("contentUploadStatus", ContentUploadStatus.PROCESSED),
                Filters.eq("contentUploadStatus", ContentUploadStatus.SUCCESS_NO_CONTENT)
        );
        Bson contentFilterRaw = Filters.eq("contentUploadStatus", ContentUploadStatus.RAW_UPLOADED);
        if (onlyProcessed) {
            filter = Filters.and(filter, contentFilter);
        } else {
            filter = Filters.and(
                    filter,
                    Filters.or(contentFilter, contentFilterRaw));
        }

        if (lastPostId.isPresent()) {
            filter = Filters.and(
                    filter,
                    lt("_id", new ObjectId(lastPostId.get()))
            );
        }

        FindIterable<Document> docs = collection.find(fromUserIdFilter)
                .sort(descending("_id")) // _id contains the create time as well
                .limit(limit);

        List<Post> posts = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                posts.add(serde.fromJson(doc.toJson(), Post.class));
            }
        }
        return posts;
    }

    public List<Post> getPostOfGod(String godId,
                                   String godName,
                                   int limit,
                                   Optional<String> lastPostId,
                                   boolean onlyProcessed) {
        MongoCollection<Document> collection = getCollection();

        Bson godIdFilter = eq("associatedGodId", new ObjectId(godId));

        Bson godNameTagFilter = eq("tag", godName);
        Bson contentTypeFilterForTaggedGods =
                Filters.in("type",
                        PostType.AUDIO.name(), PostType.VIDEO.name(), PostType.IMAGES.name());
        Bson godNameTagCompositeFilter = Filters.and(godNameTagFilter, contentTypeFilterForTaggedGods);

        Bson filter = Filters.and(getIsDeletedFilter(false), Filters.or(godIdFilter, godNameTagCompositeFilter));

        Bson contentFilterProcessed = Filters.or(
                Filters.eq("contentUploadStatus", ContentUploadStatus.PROCESSED),
                Filters.eq("contentUploadStatus", ContentUploadStatus.SUCCESS_NO_CONTENT)
        );
        Bson contentFilterRaw = Filters.eq("contentUploadStatus", ContentUploadStatus.RAW_UPLOADED);
        if (onlyProcessed) {
            filter = Filters.and(filter, contentFilterProcessed);
        } else {
            filter = Filters.and(
                    filter,
                    Filters.or(contentFilterProcessed, contentFilterRaw));
        }


        if (lastPostId.isPresent()) {
            filter = Filters.and(
                    filter,
                    lt("_id", new ObjectId(lastPostId.get()))
            );
        }

        FindIterable<Document> docs = collection.find(filter)
                .sort(descending("_id")) // _id contains the create time as well
                .limit(limit);

        List<Post> posts = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                posts.add(serde.fromJson(doc.toJson(), Post.class));
            }
        }
        return posts;
    }

    public List<Post> getPosts(String associationId, PostAssociationType associationType, int limit) {
        MongoCollection<Document> collection = getCollection();
        Bson contentFilter = Filters.or(
                Filters.eq("contentUploadStatus", ContentUploadStatus.PROCESSED),
                Filters.eq("contentUploadStatus", ContentUploadStatus.SUCCESS_NO_CONTENT)
        );

        Bson idFilter;
        switch (associationType) {
            case MANDIR:
                idFilter = Filters.eq("associatedMandirId", new ObjectId(associationId));
                break;
            case INFLUENCER:
                idFilter = Filters.eq("associatedInfluencerId", new ObjectId(associationId));
                break;
            case GOD:
                idFilter = Filters.eq("associatedGodId", new ObjectId(associationId));
                break;
            default:
                throw new IllegalStateException("unhandled association type:" + associationType);
        }
        Bson filter = Filters.and(
                idFilter,
                contentFilter,
                getIsDeletedFilter(false)
        );

        log.info("getPosts by association filter:" + filter);

        FindIterable<Document> docs = collection.find(filter)
                .sort(descending("_id")) // _id contains the create time as well
                .limit(limit);

        List<Post> posts = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                posts.add(serde.fromJson(doc.toJson(), Post.class));
            }
        }
        log.info("getPosts by association posts:" + posts.stream().map(Post::getPostId).toList());

        return posts;
    }

    public List<Post> getPostsPaginated(String associationId, PostAssociationType associationType, int limit,
                                        Optional<String> lastPostId,
                                        boolean onlyProcessed) {
        MongoCollection<Document> collection = getCollection();
        Bson associationIdFilter;
        switch (associationType) {
            case MANDIR:
                associationIdFilter = Filters.eq("associatedMandirId", new ObjectId(associationId));
                break;
            case INFLUENCER:
                associationIdFilter = Filters.eq("associatedInfluencerId", new ObjectId(associationId));
                break;
            case GOD:
                associationIdFilter = Filters.eq("associatedGodId", new ObjectId(associationId));
                break;
            default:
                throw new IllegalStateException("unhandled association type:" + associationType);
        }

        Bson filter = Filters.and(getIsDeletedFilter(false), associationIdFilter);

        Bson contentFilterProcessed = Filters.or(
                Filters.eq("contentUploadStatus", ContentUploadStatus.PROCESSED),
                Filters.eq("contentUploadStatus", ContentUploadStatus.SUCCESS_NO_CONTENT)
        );
        Bson contentFilterRaw = Filters.eq("contentUploadStatus", ContentUploadStatus.RAW_UPLOADED);

        if (onlyProcessed) {
            filter = Filters.and(filter, contentFilterProcessed);
        } else {
            filter = Filters.and(
                    filter,
                    Filters.or(contentFilterProcessed, contentFilterRaw));
        }

        if (lastPostId.isPresent()) {
            filter = Filters.and(
                    filter,
                    lt("_id", new ObjectId(lastPostId.get()))
            );
        }

        FindIterable<Document> docs = collection.find(filter)
                .sort(descending("_id")) // _id contains the create time as well
                .limit(limit);

        List<Post> posts = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                posts.add(serde.fromJson(doc.toJson(), Post.class));
            }
        }

        return posts;
    }

    public List<Post> getPostsByType(PostType postType, int limit, Optional<String> lastPostId) {
        MongoCollection<Document> collection = getCollection();

        Bson contentFilter = Filters.or(
                Filters.eq("contentUploadStatus", ContentUploadStatus.PROCESSED),
                Filters.eq("contentUploadStatus", ContentUploadStatus.SUCCESS_NO_CONTENT)
        );

        Bson finalFilter = getFinalFilter(postType, lastPostId, contentFilter, false);
        finalFilter = Filters.and(getIsDeletedFilter(false), finalFilter);

        FindIterable<Document> docs = collection.find(finalFilter)
                .sort(descending("_id")) // _id contains the create time as well
                .limit(limit);

        List<Post> posts = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                posts.add(serde.fromJson(doc.toJson(), Post.class));
            }
        }

        return posts;
    }

    public List<Post> getPostsByTypeAndStatus(PostType postType, int limit,
                                              Optional<String> lastPostId,
                                              ContentUploadStatus status) {
        MongoCollection<Document> collection = getCollection();
        Bson contentFilter = Filters.eq("contentUploadStatus", status.name());
        Bson finalFilter = getFinalFilter(postType, lastPostId, contentFilter, true);
        finalFilter = Filters.and(getIsDeletedFilter(false), finalFilter);

        FindIterable<Document> docs = collection.find(finalFilter)
                .sort(ascending("_id")) // _id contains the create time as well
                .limit(limit);

        List<Post> posts = new ArrayList<>();

        try (MongoCursor<Document> iter = docs.iterator()) {
            while (iter.hasNext()) {
                Document doc = iter.next();
                posts.add(serde.fromJson(doc.toJson(), Post.class));
            }
        }

        return posts;
    }

    public int countPostsForInfluencer(String influencerId) {
        MongoCollection<Document> collection = getCollection();

        Bson associationFilter = Filters.eq("associatedInfluencerId", new ObjectId(influencerId));
        ;
        Bson fromUserIdFilter = Filters.eq("fromUserId", new ObjectId(influencerId));

        Bson contentFilterProcessed = Filters.or(
                Filters.eq("contentUploadStatus", ContentUploadStatus.PROCESSED),
                Filters.eq("contentUploadStatus", ContentUploadStatus.SUCCESS_NO_CONTENT)
        );

        Bson filter = Filters.and(Filters.or(associationFilter, fromUserIdFilter), contentFilterProcessed);
        filter = Filters.and(getIsDeletedFilter(false), filter);

        long count = collection.countDocuments(filter);

        return (int) count;
    }

    public void initCollectionAndIndexes() {
        MongoDatabase db = mongoClient.getDatabase(database);
        db.createCollection(POSTS_COLLECTION);

        MongoCollection<Document> collection = db.getCollection(POSTS_COLLECTION);

        IndexOptions indexOptions = new IndexOptions()
                .name("mandir_id_index");
        String resultCreateIndex = collection.createIndex(Indexes.descending("contentUploadStatus", "associatedMandirId", "_id"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));

        indexOptions = new IndexOptions()
                .name("god_id_index");
        resultCreateIndex = collection.createIndex(Indexes.descending("contentUploadStatus", "associatedGodId", "_id"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));

        indexOptions = new IndexOptions()
                .name("influencer_id_index");
        resultCreateIndex = collection.createIndex(Indexes.descending("contentUploadStatus", "associatedInfluencerId", "_id"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));

        indexOptions = new IndexOptions()
                .name("fromUserId_index");
        resultCreateIndex = collection.createIndex(Indexes.descending("contentUploadStatus", "fromUserId", "_id"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));

        indexOptions = new IndexOptions()
                .name("postType_index");
        resultCreateIndex = collection.createIndex(Indexes.descending("contentUploadStatus", "type", "_id"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));

        indexOptions = new IndexOptions()
                .name("god_tag_index");
        resultCreateIndex = collection.createIndex(Indexes.descending("contentUploadStatus", "tag", "type", "_id"),
                indexOptions);
        log.info(String.format("Index created: %s", resultCreateIndex));

    }

    private Bson getIsDeletedFilter(boolean isDeleted) {
        if (!isDeleted) {
            return Filters.or(Filters.exists("isDeleted", isDeleted), Filters.eq("isDeleted", isDeleted));
        }

        return Filters.eq("isDeleted", isDeleted);
    }

    private static Bson getFinalFilter(PostType postType, Optional<String> lastPostId, Bson contentFilter,
                                       boolean isAscending) {

        Bson postTypeFilter = Filters.eq("type", postType.name());

        Bson finalFilter = Filters.and(
                contentFilter,
                postTypeFilter
        );

        if (lastPostId.isPresent()) {
            Bson postIdFilter = isAscending ?
                    gt("_id", new ObjectId(lastPostId.get())) :
                    lt("_id", new ObjectId(lastPostId.get()));

            finalFilter = Filters.and(finalFilter, postIdFilter);
        }
        return finalFilter;
    }

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(POSTS_COLLECTION);
    }
}
