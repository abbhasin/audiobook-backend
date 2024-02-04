package com.enigma.audiobook.backend.jobs;

import com.enigma.audiobook.backend.aws.S3Proxy;
import com.enigma.audiobook.backend.dao.PostsDao;
import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.Darshan;
import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.models.PostType;
import com.enigma.audiobook.backend.utils.SerDe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import static com.enigma.audiobook.backend.dao.DarshanDao.DARSHAN_REG_COLLECTION;
import static com.enigma.audiobook.backend.dao.PostsDao.POSTS_COLLECTION;
import static com.enigma.audiobook.backend.utils.ObjectStoreMappingUtils.getPostVideoUploadObjectKeyFormatProcessed;

@Slf4j
@RequiredArgsConstructor
public class ContentEncodingHandler {
    private static final SerDe serde = new SerDe();
    static String bucket_url = "https://one-god-dev.s3.ap-south-1.amazonaws.com";
    static String bucket = "one-god-dev";

    private final S3Proxy s3Proxy;
    private final PostsDao postsDao;

    public void encodeContentForCollectionEntry(String db, String collection, String collectionEntry) {
        switch (collection) {
            case POSTS_COLLECTION:
                Post post = serde.fromJson(collectionEntry, Post.class);
                handlePost(post);
                break;
            case DARSHAN_REG_COLLECTION:
                Darshan darshan = serde.fromJson(collectionEntry, Darshan.class);
                break;
            default:
                log.info("unhandled collection:" + collection);
                return;
        }
    }

    private void handlePost(Post post) {
        if (post.getContentUploadStatus().equals(ContentUploadStatus.PROCESSED) ||
                post.getContentUploadStatus().equals(ContentUploadStatus.SUCCESS_NO_CONTENT)) {
            return;
        }
        switch (post.getType()) {
            case VIDEO:
                String outputS3KeyFormat = getPostVideoUploadObjectKeyFormatProcessed(post.getPostId(), post.getFromUserId());
                handleVideo(post.getPostId(), post.getVideoUrl(), outputS3KeyFormat);
                break;
            case AUDIO:
                break;
            case IMAGES:
                break;
            default:
                return;
        }
    }

    private void handleVideo(String postId, String inputVideoUrl, String outputS3KeyFormat) {
        URI inputVideoURI = URI.create(inputVideoUrl);
        String inputVideoLocalFilePathPrefix = "/tmp/one-god-local/input";
        String outputVideoLocalFilePathPrefix = "/tmp/one-god-local/output";
        try {
            // fetch the object to local
            String inputVideoLocalFilePath = String.format(inputVideoLocalFilePathPrefix + "/%s",
                    inputVideoURI.getPath().substring(1)); // remove prefix '/'
            s3Proxy.getObject(inputVideoUrl, URI.create(inputVideoLocalFilePath));

            String outputDir = String.format(outputVideoLocalFilePathPrefix + "/%s",
                    inputVideoURI.getPath().substring(1, inputVideoURI.getPath().lastIndexOf("/"))); // remove prefix '/'

            // create HLS files
            ContentEncoderV2.updateVideoContentToDir(inputVideoLocalFilePath, outputDir);

            // upload to s3
            File outputDirFile = new File(outputDir);
            File[] outputFiles = Objects.requireNonNull(outputDirFile.listFiles());
            String finalS3ContentURL = null;

            for (File f : outputFiles) {
                String name = f.getName();
                String s3OutputObjectKey = String.format(outputS3KeyFormat, name);

                if (s3OutputObjectKey.endsWith("m3u8")) {
                    finalS3ContentURL = getObjectUrl(s3OutputObjectKey);
                }

                Optional<String> contentType = name.endsWith("m3u8") ? Optional.of("application/x-mpegURL") :
                        Optional.of("video/MP2T");
                s3Proxy.putObject(bucket, s3OutputObjectKey, f, contentType);
            }

            // update db
            postsDao.updatePost(postId, ContentUploadStatus.PROCESSED, PostType.VIDEO,
                    null, finalS3ContentURL,
                    null,
                    null);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // remove staged data
            removeStagedData(inputVideoLocalFilePathPrefix);
            removeStagedData(outputVideoLocalFilePathPrefix);
        }
    }

    private void removeStagedData(String dir) {
        File f = new File(dir);
        f.deleteOnExit();
        try {
            FileUtils.deleteDirectory(new File("directory"));
        } catch (IOException e) {
            log.error("unable to delete dir:" + dir, e);
            // throw new RuntimeException(e);
        }
    }

    public String getObjectUrl(String objectKey) {
        return String.format("%s/%s", bucket_url, objectKey);
    }
}
