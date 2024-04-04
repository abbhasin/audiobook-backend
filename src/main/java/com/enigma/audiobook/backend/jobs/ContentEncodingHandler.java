package com.enigma.audiobook.backend.jobs;

import com.enigma.audiobook.backend.aws.S3Proxy;
import com.enigma.audiobook.backend.dao.DarshanDao;
import com.enigma.audiobook.backend.dao.PostsDao;
import com.enigma.audiobook.backend.jobs.algo.DarshanContentTransformer;
import com.enigma.audiobook.backend.jobs.algo.PostsContentTransformer;
import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.Darshan;
import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.utils.SerDe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static com.enigma.audiobook.backend.dao.DarshanDao.DARSHAN_REG_COLLECTION;
import static com.enigma.audiobook.backend.dao.PostsDao.POSTS_COLLECTION;
import static com.enigma.audiobook.backend.jobs.algo.BaseContentTransformer.removeStagedData;

@Slf4j
@RequiredArgsConstructor
@Component
public class ContentEncodingHandler {
    private static final SerDe serde = new SerDe();
    final String bucketUrl;
    final String bucket;
    final String inputContentLocalFilePathPrefixWOScheme;
    final String outputContentLocalFilePathPrefixWOScheme;
    final S3Proxy s3Proxy;
    final PostsDao postsDao;
    final DarshanDao darshanDao;

    @Autowired
    public ContentEncodingHandler(S3Proxy s3Proxy,
                                  @Value("${s3-config.bucket_url}") String bucketUrl,
                                  @Value("${s3-config.bucket}") String bucket,
                                  @Value("${content-transformer-config.inputContentLocalFilePathPrefixWOScheme}")
                                  String inputContentLocalFilePathPrefixWOScheme,
                                  @Value("${content-transformer-config.outputContentLocalFilePathPrefixWOScheme}")
                                  String outputContentLocalFilePathPrefixWOScheme,
                                  PostsDao postsDao, DarshanDao darshanDao) {
        this.bucketUrl = bucketUrl;
        this.bucket = bucket;
        this.inputContentLocalFilePathPrefixWOScheme = inputContentLocalFilePathPrefixWOScheme;
        this.outputContentLocalFilePathPrefixWOScheme = outputContentLocalFilePathPrefixWOScheme;
        this.s3Proxy = s3Proxy;
        this.postsDao = postsDao;
        this.darshanDao = darshanDao;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.info("running shutdown hook for encoder");
                removeStagedData(Arrays.asList(inputContentLocalFilePathPrefixWOScheme, outputContentLocalFilePathPrefixWOScheme));
            }
        });
    }

    public void encodeContentForCollectionEntry(String db, String collection, String collectionEntry) {
        switch (collection) {
            case POSTS_COLLECTION:
                Post post = serde.fromJson(collectionEntry, Post.class);
                if (!post.getContentUploadStatus().equals(ContentUploadStatus.RAW_UPLOADED)) {
                    return;
                }

                PostsContentTransformer postsContentTransformer =
                        new PostsContentTransformer(s3Proxy, postsDao, bucketUrl,
                                bucket, inputContentLocalFilePathPrefixWOScheme,
                                outputContentLocalFilePathPrefixWOScheme);

                postsContentTransformer.handlePost(post);
                break;
            case DARSHAN_REG_COLLECTION:
                Darshan darshan = serde.fromJson(collectionEntry, Darshan.class);
                if (!darshan.getVideoUploadStatus().equals(ContentUploadStatus.RAW_UPLOADED)) {
                    return;
                }

                DarshanContentTransformer darshanContentTransformer =
                        new DarshanContentTransformer(s3Proxy, darshanDao, bucketUrl, bucket,
                                inputContentLocalFilePathPrefixWOScheme,
                                outputContentLocalFilePathPrefixWOScheme);
                darshanContentTransformer.handleDarshan(darshan);
                break;
            default:
                log.info("unhandled collection:" + collection);
                return;
        }
    }
}
