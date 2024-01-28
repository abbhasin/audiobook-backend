package com.enigma.audiobook.backend.utils;

import com.enigma.audiobook.backend.models.Post;

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

    public static String getDarshanVideoUploadDirS3Url(String postId) {
        return String.format("s3://one-god-dev/darshans/video/%s/raw/", postId);
    }

    public static String getMandirImageUploadDirS3Url(String mandirId) {
        return String.format("s3://one-god-dev/mandirs/images/%s/", mandirId);
    }

    public static String getPostVideoUploadObjectKeyFormat(String postId, String userId) {
        return String.format("posts/videos/user/%s/video/%s/raw/", userId, postId) + "%s";
    }

    public static String getPostImageUploadObjectKeyFormat(String postId, String userId) {
        return String.format("posts/images/user/%s/image/%s/raw/", userId, postId) + "%s";
    }

    public static String getPostAudioUploadObjectKeyFormat(String postId, String userId) {
        return String.format("posts/audios/user/%s/audio/%s/raw/", userId, postId) + "%s";
    }

    public static String getDarshanVideoUploadObjectKeyFormat(String darshanId) {
        return String.format("darshans/video/%s/raw/", darshanId) + "%s";
    }
}
