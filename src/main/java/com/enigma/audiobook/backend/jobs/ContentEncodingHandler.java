package com.enigma.audiobook.backend.jobs;

import com.enigma.audiobook.backend.models.Darshan;
import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.utils.SerDe;
import com.enigma.audiobook.backend.jobs.algo.DarshanContentTransformer;
import com.enigma.audiobook.backend.jobs.algo.PostsContentTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.enigma.audiobook.backend.dao.DarshanDao.DARSHAN_REG_COLLECTION;
import static com.enigma.audiobook.backend.dao.PostsDao.POSTS_COLLECTION;

@Slf4j
@RequiredArgsConstructor
@Component
public class ContentEncodingHandler {
    private static final SerDe serde = new SerDe();
    static String bucket_url = "https://one-god-dev.s3.ap-south-1.amazonaws.com";
    static String bucket = "one-god-dev";

    private final PostsContentTransformer postsContentTransformer;
    private final DarshanContentTransformer darshanContentTransformer;

    public void encodeContentForCollectionEntry(String db, String collection, String collectionEntry) {
        switch (collection) {
            case POSTS_COLLECTION:
                Post post = serde.fromJson(collectionEntry, Post.class);
                postsContentTransformer.handlePost(post);
                break;
            case DARSHAN_REG_COLLECTION:
                Darshan darshan = serde.fromJson(collectionEntry, Darshan.class);
                darshanContentTransformer.handleDarshan(darshan);
                break;
            default:
                log.info("unhandled collection:" + collection);
                return;
        }
    }
}
