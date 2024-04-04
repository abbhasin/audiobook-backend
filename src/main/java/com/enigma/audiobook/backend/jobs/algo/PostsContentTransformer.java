package com.enigma.audiobook.backend.jobs.algo;

import com.enigma.audiobook.backend.aws.S3Proxy;
import com.enigma.audiobook.backend.dao.PostsDao;
import com.enigma.audiobook.backend.jobs.ContentEncoderV2;
import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.Post;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.enigma.audiobook.backend.utils.ObjectStoreMappingUtils.*;

@Slf4j
public class PostsContentTransformer extends BaseContentTransformer {
    final PostsDao postsDao;
    volatile Post post;

    public PostsContentTransformer(S3Proxy s3Proxy, PostsDao postsDao,
                                   String bucket_url,
                                   String bucket,
                                   String inputContentLocalFilePathPrefixWOScheme,
                                   String outputContentLocalFilePathPrefixWOScheme) {
        super(bucket_url, bucket,
                inputContentLocalFilePathPrefixWOScheme,
                outputContentLocalFilePathPrefixWOScheme,
                s3Proxy);
        this.postsDao = postsDao;
        this.post = null;
    }

    public void handlePost(Post post) {
        this.post = post;
        log.info("handling post:{}", post);
        String outputS3KeyFormat = null;
        switch (post.getType()) {
            case VIDEO:
                outputS3KeyFormat = getPostVideoUploadObjectKeyFormatProcessed(post.getPostId(), post.getFromUserId());
                handleContent(Collections.singletonList(post.getVideoUrl()), outputS3KeyFormat);
                break;
            case AUDIO:
                outputS3KeyFormat = getPostAudioUploadObjectKeyFormatProcessed(post.getPostId(), post.getFromUserId());
                handleContent(Collections.singletonList(post.getAudioUrl()), outputS3KeyFormat);
                break;
            case IMAGES:
                outputS3KeyFormat = getPostImageUploadObjectKeyFormatProcessed(post.getPostId(), post.getFromUserId());
                handleContent(post.getImagesUrl(), outputS3KeyFormat);
                break;
            default:
                return;
        }
    }

    @Override
    protected void updateDBEntry() {
        log.info("updating db for post:{}", post);
        switch (post.getType()) {
            case VIDEO:
                Preconditions.checkState(post.getVideoUrl().contains(post.getPostId()));
                Preconditions.checkState(post.getThumbnailUrl().contains(post.getPostId()));
                Preconditions.checkState(post.getVideoUrl().endsWith("m3u8"));
                Preconditions.checkState(post.getThumbnailUrl().endsWith("jpg"));
                Preconditions.checkState(post.getVideoUrl().contains("processed/"));
                Preconditions.checkState(post.getThumbnailUrl().contains("processed/"));
                break;
            case AUDIO:
                Preconditions.checkState(post.getAudioUrl().contains(post.getPostId()));
                Preconditions.checkState(post.getAudioUrl().endsWith("m3u8"));
                Preconditions.checkState(post.getAudioUrl().contains("processed/"));
                break;
            default:
                throw new IllegalStateException("unhandled type:" + post.getType());
        }

        postsDao.updatePost(post.getPostId(), ContentUploadStatus.PROCESSED, post.getType(),
                post.getThumbnailUrl(), post.getVideoUrl(),
                post.getImagesUrl(),
                post.getAudioUrl());
    }

    @Override
    protected Optional<String> getContentType(String fileName) {
        log.info("get content type for file:{}", fileName);
        if (fileName.endsWith("m3u8")) {
            return Optional.of("application/x-mpegURL");
        } else if (fileName.endsWith("ts")) {
            return Optional.of("video/MP2T");
        } else if (fileName.endsWith("jpg")) {
            return Optional.of("image/jpeg");
        }

        return Optional.empty();
    }

    @Override
    protected void encodeContentToDir(String inputContentLocalFilePath, String outputDir) throws Exception {
        log.info("encode content, inputContentLocalFilePath:{}, outputDir:{}, post:{}",
                inputContentLocalFilePath, outputDir, post);
        switch (post.getType()) {
            case VIDEO:
                ContentEncoderV2.updateVideoContentToDir(inputContentLocalFilePath, outputDir);
                ContentEncoderV2.generateThumbnailToDir(inputContentLocalFilePath, outputDir);
                break;
            case AUDIO:
                ContentEncoderV2.updateAudioContentToDir(inputContentLocalFilePath, outputDir);
                break;
            case IMAGES:
                ContentEncoderV2.updateImageContentToDir(inputContentLocalFilePath, outputDir);
                break;
            default:
                return;
        }
    }

    @Override
    protected void addToUploadsList(String fileName, String s3ObjectURL) {
        switch (post.getType()) {
            case VIDEO:
                if (fileName.endsWith("m3u8")) {
                    log.info("add to upload list:{}, url:{}", fileName, s3ObjectURL);
                    post.setVideoUrl(s3ObjectURL);
                } else if (fileName.endsWith("jpg")) {
                    log.info("add to upload list:{}, url:{}", fileName, s3ObjectURL);
                    post.setThumbnailUrl(s3ObjectURL);
                }
                break;
            case AUDIO:
                if (fileName.endsWith("m3u8")) {
                    log.info("add to upload list:{}, url:{}", fileName, s3ObjectURL);
                    post.setAudioUrl(s3ObjectURL);
                }
                break;
            case IMAGES:
                if (fileName.endsWith("jpg")) {
                    log.info("add to upload list:{}, url:{}", fileName, s3ObjectURL);
                    List<String> images =
                            post.getImagesUrl().stream().anyMatch(img -> img.contains("raw/")) ? new ArrayList<>()
                            : post.getImagesUrl();
                    images.add(s3ObjectURL);
                    post.setImagesUrl(images);
                }
                break;
            default:
                return;
        }
    }
}
