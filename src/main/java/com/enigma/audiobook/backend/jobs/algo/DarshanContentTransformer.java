package com.enigma.audiobook.backend.jobs.algo;

import com.enigma.audiobook.backend.aws.S3Proxy;
import com.enigma.audiobook.backend.dao.DarshanDao;
import com.enigma.audiobook.backend.jobs.ContentEncoderV2;
import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.Darshan;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static com.enigma.audiobook.backend.utils.ObjectStoreMappingUtils.getDarshanVideoUploadObjectKeyFormatProcessed;

@Slf4j
public class DarshanContentTransformer extends BaseContentTransformer {
    final DarshanDao darshanDao;
    volatile Darshan darshan;

    public DarshanContentTransformer(S3Proxy s3Proxy, DarshanDao darshanDao,
                                     String bucket_url,
                                     String bucket,
                                     String inputContentLocalFilePathPrefixWOScheme,
                                     String outputContentLocalFilePathPrefixWOScheme) {
        super(bucket_url, bucket,
                inputContentLocalFilePathPrefixWOScheme,
                outputContentLocalFilePathPrefixWOScheme,
                s3Proxy);
        this.darshanDao = darshanDao;
    }

    public void handleDarshan(Darshan darshan) {
        this.darshan = darshan;
        log.info("handling darshan:{}", darshan);
        String outputS3KeyFormat = getDarshanVideoUploadObjectKeyFormatProcessed(darshan.getDarshanId());
        handleContent(Collections.singletonList(darshan.getVideoUrl()), outputS3KeyFormat);
    }

    @Override
    protected void updateDBEntry() {
        log.info("updating db for darshan:{}", darshan);
        Preconditions.checkState(darshan.getVideoUrl().endsWith("m3u8"));
        Preconditions.checkState(darshan.getThumbnailUrl().endsWith("jpg"));
        Preconditions.checkState(darshan.getVideoUrl().contains("processed/"));
        Preconditions.checkState(darshan.getThumbnailUrl().contains("processed/"));
        Preconditions.checkState(darshan.getVideoUrl().contains(darshan.getDarshanId()));
        Preconditions.checkState(darshan.getThumbnailUrl().contains(darshan.getDarshanId()));
        darshanDao.updateDarshan(darshan.getDarshanId(), darshan.getThumbnailUrl(), darshan.getVideoUrl(),
                ContentUploadStatus.PROCESSED);
    }

    @Override
    protected Optional<String> getContentType(String fileName) {
        log.info("get content type for:{}", fileName);
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
    protected void encodeContentToDir(String inputContentLocalFilePath, String outputDir) throws IOException, InterruptedException, Exception {
        log.info("encoding content inputContentLocalFilePath:{}, outputDir:{}", inputContentLocalFilePath, outputDir);
        ContentEncoderV2.updateVideoContentToDir(inputContentLocalFilePath, outputDir);
        ContentEncoderV2.generateThumbnailToDir(inputContentLocalFilePath, outputDir);
    }

    @Override
    protected void addToUploadsList(String fileName, String s3ObjectURL) {
        if (fileName.endsWith("m3u8")) {
            log.info("add to upload list, file:{}, url:{}", fileName, s3ObjectURL);
            darshan.setVideoUrl(s3ObjectURL);
        } else if (fileName.endsWith("jpg")) {
            log.info("add to upload list, file:{}, url:{}", fileName, s3ObjectURL);
            darshan.setThumbnailUrl(s3ObjectURL);
        }
    }
}
