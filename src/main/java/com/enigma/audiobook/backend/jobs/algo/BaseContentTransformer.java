package com.enigma.audiobook.backend.jobs.algo;

import com.enigma.audiobook.backend.aws.S3Proxy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

@Slf4j
public abstract class BaseContentTransformer {
//    static String inputContentLocalFilePathPrefix = "file:///Users/akhil/Downloads/tmp/one-god-local/input";
//    static String outputContentLocalFilePathPrefix = "file:///Users/akhil/Downloads/tmp/one-god-local/output";

    final String bucketUrl;
    final String bucket;
    final String inputContentLocalFilePathPrefixWOScheme;
    final String outputContentLocalFilePathPrefixWOScheme;
    final S3Proxy s3Proxy;

    protected BaseContentTransformer(String bucketUrl, String bucket, String inputContentLocalFilePathPrefixWOScheme, String outputContentLocalFilePathPrefixWOScheme, S3Proxy s3Proxy) {
        this.bucketUrl = bucketUrl;
        this.bucket = bucket;
        this.inputContentLocalFilePathPrefixWOScheme = inputContentLocalFilePathPrefixWOScheme;
        this.outputContentLocalFilePathPrefixWOScheme = outputContentLocalFilePathPrefixWOScheme;
        this.s3Proxy = s3Proxy;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.info("running shutdown hook for encoder");
                removeStagedData(Arrays.asList(inputContentLocalFilePathPrefixWOScheme, outputContentLocalFilePathPrefixWOScheme));
            }
        });
    }


    protected void handleContent(List<String> inputContentUrls, String outputS3KeyFormat) {
        List<String> inputDirs = new ArrayList<>();
        List<String> outputDirs = new ArrayList<>();
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
                inputDirs.add(inputContentLocalFilePathDir);
                s3Proxy.getObject(inputContentUrl, new File(inputContentLocalFilePath).toURI());

                String outputDir = String.format(outputContentLocalFilePathPrefixWOScheme + "/%s",
                        inputContentURI.getPath().substring(1, inputContentURI.getPath().lastIndexOf("/"))); // remove prefix '/'
                log.info("outputDir:{}", outputDir);
                createDir(outputDir);
                outputDirs.add(outputDir);

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
            removeStagedData(inputDirs);
            removeStagedData(outputDirs);
        }
    }

    private void createDir(String dir) {
        File file = new File(dir);
        file.deleteOnExit();
        boolean dirCreated = file.mkdirs();
        log.info("dirCreated:{}, exists:{}", dirCreated, file.exists());
        if (!file.exists()) {
            throw new IllegalStateException("unable to create dir:" + dir);
        }

    }

    private void removeStagedData(List<String> dirs) {
        for (String dir : dirs) {
            File f = new File(dir);
            f.deleteOnExit();
            try {
                FileUtils.deleteDirectory(f);
            } catch (IOException e) {
                log.error("unable to delete dir:" + dir, e);
                // throw new RuntimeException(e);
            }
        }
    }

    private void removeFromS3(List<String> inputContentUrls) {
        for (String inputUri : inputContentUrls) {
            s3Proxy.markForExpiration(inputUri);
        }
    }

    protected abstract void updateDBEntry();

    protected abstract Optional<String> getContentType(String fileName);

    protected abstract void encodeContentToDir(String inputContentLocalFilePath, String outputDir) throws IOException, InterruptedException, Exception;

    protected abstract void addToUploadsList(String fileName, String s3ObjectURL);

    public String getObjectUrl(String objectKey) {
        return String.format("%s/%s", bucketUrl, objectKey);
    }
}
