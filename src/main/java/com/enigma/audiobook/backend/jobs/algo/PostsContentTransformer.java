package com.enigma.audiobook.backend.jobs.algo;

import com.enigma.audiobook.backend.aws.S3Proxy;
import com.enigma.audiobook.backend.dao.PostsDao;
import com.enigma.audiobook.backend.jobs.ContentEncoderV2;
import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.Post;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.enigma.audiobook.backend.utils.ObjectStoreMappingUtils.*;

@Component
public class PostsContentTransformer extends BaseContentTransformer {
    final PostsDao postsDao;
    Post post;

    public PostsContentTransformer(S3Proxy s3Proxy, PostsDao postsDao) {
        super(s3Proxy);
        this.postsDao = postsDao;
        this.post = null;
    }

    public void handlePost(Post post) {
        this.post = post;
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
        postsDao.updatePost(post.getPostId(), ContentUploadStatus.PROCESSED, post.getType(),
                post.getThumbnailUrl(), post.getVideoUrl(),
                post.getImagesUrl(),
                post.getAudioUrl());
    }

    @Override
    protected Optional<String> getContentType(String fileName) {
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
        switch (post.getType()) {
            case VIDEO:
                ContentEncoderV2.updateVideoContentToDir(inputContentLocalFilePath, outputDir);
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
                    post.setVideoUrl(s3ObjectURL);
                } else if (fileName.endsWith("jpg")) {
                    post.setThumbnailUrl(s3ObjectURL);
                }
                break;
            case AUDIO:
                if (fileName.endsWith("m3u8")) {
                    post.setAudioUrl(s3ObjectURL);
                }
                break;
            case IMAGES:
                if (fileName.endsWith("jpg")) {
                    List<String> images = post.getImagesUrl() == null ? new ArrayList<>() : post.getImagesUrl();
                    images.add(s3ObjectURL);
                    post.setImagesUrl(images);
                }
                break;
            default:
                return;
        }
    }
}
