package com.enigma.audiobook.backend.utils.algo;

import com.enigma.audiobook.backend.aws.S3Proxy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
public abstract class BaseContentTransformer {
    static String bucket_url = "https://one-god-dev.s3.ap-south-1.amazonaws.com";
    static String bucket = "one-god-dev";
    static String inputAudioLocalFilePathPrefix = "/tmp/one-god-local/input";
    static String outputAudioLocalFilePathPrefix = "/tmp/one-god-local/output";

    S3Proxy s3Proxy;


    protected void handleContent(List<String> inputContentUrls, String outputS3KeyFormat) {

        try {
            for (String inputContentUrl : inputContentUrls) {
                URI inputContentURI = URI.create(inputContentUrl);

                // fetch the object to local
                String inputContentLocalFilePath = String.format(inputAudioLocalFilePathPrefix + "/%s",
                        inputContentURI.getPath().substring(1)); // remove prefix '/'

                s3Proxy.getObject(inputContentUrl, URI.create(inputContentLocalFilePath));

                String outputDir = String.format(outputAudioLocalFilePathPrefix + "/%s",
                        inputContentURI.getPath().substring(1, inputContentURI.getPath().lastIndexOf("/"))); // remove prefix '/'

                // create HLS files
                encodeContentToDir(inputContentLocalFilePath, outputDir);

                // upload to s3
                File outputDirFile = new File(outputDir);
                File[] outputFiles = Objects.requireNonNull(outputDirFile.listFiles());

                for (File f : outputFiles) {
                    String fileName = f.getName();
                    String s3OutputObjectKey = String.format(outputS3KeyFormat, fileName);
                    addToUploadsList(fileName, getObjectUrl(s3OutputObjectKey));

                    Optional<String> contentType = getContentType(fileName);
                    s3Proxy.putObject(bucket, s3OutputObjectKey, f, contentType);
                }
            }

            // update db
            updateDBEntry();
            // remove from s3
            removeFromS3(inputContentUrls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // remove staged data
            removeStagedData(inputAudioLocalFilePathPrefix);
            removeStagedData(outputAudioLocalFilePathPrefix);
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

    private void removeFromS3(List<String> inputContentUrls) {
        for (String inputUri : inputContentUrls) {
            s3Proxy.deleteObject(inputUri);
        }
    }

    protected abstract void updateDBEntry();

    protected abstract Optional<String> getContentType(String fileName);

    protected abstract void encodeContentToDir(String inputContentLocalFilePath, String outputDir) throws IOException, InterruptedException, Exception;

    protected abstract void addToUploadsList(String fileName, String s3ObjectURL);

    public String getObjectUrl(String objectKey) {
        return String.format("%s/%s", bucket_url, objectKey);
    }
}
