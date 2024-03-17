package com.enigma.audiobook.backend.jobs.algo;

import com.enigma.audiobook.backend.aws.S3Proxy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
public abstract class BaseContentTransformer {
//    static String inputContentLocalFilePathPrefix = "file:///Users/akhil/Downloads/tmp/one-god-local/input";
//    static String outputContentLocalFilePathPrefix = "file:///Users/akhil/Downloads/tmp/one-god-local/output";

    String bucket_url = "https://one-god-dev.s3.ap-south-1.amazonaws.com";
    String bucket = "one-god-dev";
    String inputContentLocalFilePathPrefixWOScheme = "/Users/akhil/Downloads/tmp/one-god-local/input";
    String outputContentLocalFilePathPrefixWOScheme = "/Users/akhil/Downloads/tmp/one-god-local/output";
    S3Proxy s3Proxy;


    protected void handleContent(List<String> inputContentUrls, String outputS3KeyFormat) {

        try {
            for (String inputContentUrl : inputContentUrls) {
                URI inputContentURI = URI.create(inputContentUrl);

                // fetch the object to local
                String inputContentLocalFilePath = String.format(inputContentLocalFilePathPrefixWOScheme + "/%s",
                        inputContentURI.getPath().substring(1)); // remove prefix '/'
                log.info("inputContentLocalFilePath:{}", inputContentLocalFilePath);
                String inputContentLocalFilePathWOScheme = String.format(inputContentLocalFilePathPrefixWOScheme + "/%s",
                        inputContentURI.getPath().substring(1)); // remove prefix '/'

                String inputContentLocalFilePathDir = inputContentLocalFilePathWOScheme.substring(0,
                        inputContentLocalFilePathWOScheme.lastIndexOf("/"));
                log.info("inputContentLocalFilePathDir:{}", inputContentLocalFilePathDir);

                createDir(inputContentLocalFilePathDir);
                s3Proxy.getObject(inputContentUrl, new File(inputContentLocalFilePath).toURI());

                String outputDir = String.format(outputContentLocalFilePathPrefixWOScheme + "/%s",
                        inputContentURI.getPath().substring(1, inputContentURI.getPath().lastIndexOf("/"))); // remove prefix '/'
                log.info("outputDir:{}", outputDir);
                createDir(outputDir);

                // create HLS files
                encodeContentToDir(inputContentLocalFilePathWOScheme, outputDir);

                // upload to s3
                File outputDirFile = new File(outputDir);
                File[] outputFiles = Objects.requireNonNull(outputDirFile.listFiles());
                for (File f : outputFiles) {
                    log.info("file path:{}, f:{}", f.getAbsolutePath(), f.toURI());
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
            removeStagedData(inputContentLocalFilePathPrefixWOScheme);
            removeStagedData(outputContentLocalFilePathPrefixWOScheme);
        }
    }

    private void createDir(String dir) {
        File file = new File(dir);
        boolean dirCreated = file.mkdirs();
        log.info("dirCreated:{}, exists:{}", dirCreated, file.exists());
        if (!file.exists()) {
            throw new IllegalStateException("unable to create dir:" + dir);
        }

    }

    private void removeStagedData(String dir) {
        File f = new File(dir);
        f.deleteOnExit();
        try {
            FileUtils.deleteDirectory(f);
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
