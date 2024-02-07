package com.enigma.audiobook.backend.jobs.algo;

import com.enigma.audiobook.backend.aws.S3Proxy;
import com.enigma.audiobook.backend.dao.DarshanDao;
import com.enigma.audiobook.backend.jobs.ContentEncoderV2;
import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.Darshan;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static com.enigma.audiobook.backend.utils.ObjectStoreMappingUtils.getDarshanVideoUploadObjectKeyFormatProcessed;

@Component
public class DarshanContentTransformer extends BaseContentTransformer {
    final DarshanDao darshanDao;
    Darshan darshan;

    public DarshanContentTransformer(S3Proxy s3Proxy, DarshanDao darshanDao) {
        super(s3Proxy);
        this.darshanDao = darshanDao;
    }

    public void handleDarshan(Darshan darshan) {
        this.darshan = darshan;
        String outputS3KeyFormat = getDarshanVideoUploadObjectKeyFormatProcessed(darshan.getDarshanId());
        handleContent(Collections.singletonList(darshan.getVideoUrl()), outputS3KeyFormat);
    }

    @Override
    protected void updateDBEntry() {
        darshanDao.updateDarshan(darshan.getDarshanId(), darshan.getThumbnailUrl(), darshan.getVideoUrl(),
                ContentUploadStatus.PROCESSED);
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
    protected void encodeContentToDir(String inputContentLocalFilePath, String outputDir) throws IOException, InterruptedException, Exception {
        ContentEncoderV2.updateVideoContentToDir(inputContentLocalFilePath, outputDir);
        ContentEncoderV2.generateThumbnailToDir(inputContentLocalFilePath, outputDir);
    }

    @Override
    protected void addToUploadsList(String fileName, String s3ObjectURL) {
        if (fileName.endsWith("m3u8")) {
            darshan.setVideoUrl(s3ObjectURL);
        } else if (fileName.endsWith("jpg")) {
            darshan.setThumbnailUrl(s3ObjectURL);
        }
    }
}
