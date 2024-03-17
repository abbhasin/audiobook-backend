package com.enigma.audiobook.backend.jobs;

import com.enigma.audiobook.backend.jobs.algo.DarshanContentTransformer;
import com.enigma.audiobook.backend.jobs.algo.PostsContentTransformer;
import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.Darshan;
import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.utils.SerDe;
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

    private final PostsContentTransformer postsContentTransformer;
    private final DarshanContentTransformer darshanContentTransformer;

    public void encodeContentForCollectionEntry(String db, String collection, String collectionEntry) {
        switch (collection) {
            case POSTS_COLLECTION:
                Post post = serde.fromJson(collectionEntry, Post.class);
                if (!post.getContentUploadStatus().equals(ContentUploadStatus.RAW_UPLOADED)) {
                    return;
                }
                postsContentTransformer.handlePost(post);
                break;
            case DARSHAN_REG_COLLECTION:
                Darshan darshan = serde.fromJson(collectionEntry, Darshan.class);
                if (!darshan.getVideoUploadStatus().equals(ContentUploadStatus.RAW_UPLOADED)) {
                    return;
                }
                darshanContentTransformer.handleDarshan(darshan);
                break;
            default:
                log.info("unhandled collection:" + collection);
                return;
        }
    }
}
