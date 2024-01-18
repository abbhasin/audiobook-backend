package com.enigma.audiobook.backend.models.requests;

import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.PostType;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Data
public class PostContentUploadReq {
    String postId;
    String fromUserId;
    List<String> imagesUrl;
    String thumnailUrl;
    String videoUrl;
    String audioUrl;
    ContentUploadStatus contentUploadStatus;

    public PostType getPostType() {
        if (videoUrl != null) {
            return PostType.VIDEO;
        } else if (audioUrl != null) {
            return PostType.AUDIO;
        } else if (CollectionUtils.isNotEmpty(imagesUrl)) {
            return PostType.IMAGES;
        }
        return PostType.TEXT;
    }
}
