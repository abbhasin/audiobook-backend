package com.enigma.audiobook.backend.utils;

public class ObjectStoreMappingUtils {

    public static String getInfluencerImageUploadDirS3Url(String influencerUserId) {
        return String.format("s3://one-god-dev/influencers/images/%s/", influencerUserId);
    }

    public static String getGodImageUploadDirS3Url(String godId) {
        return String.format("s3://one-god-dev/gods/images/%s/", godId);
    }

    public static String getPostImageUploadDirS3Url(String postId) {
        return String.format("s3://one-god-dev/posts/images/%s/", postId);
    }

    public static String getPostAudioUploadDirS3Url(String postId) {
        return String.format("s3://one-god-dev/posts/audio/%s/raw/", postId);
    }

    public static String getPostVideoUploadDirS3Url(String postId) {
        return String.format("s3://one-god-dev/posts/video/%s/raw/", postId);
    }

    public static String getPostVideoUploadDirS3UrlPostProcessing(String postId) {
        return String.format("s3://one-god-dev/posts/video/%s/processed/", postId);
    }
}
