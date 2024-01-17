package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.Post;
import lombok.Data;

@Data
public class PostInitResponse {
    final String imagesDirUrlToUpload;
    final String videoUrlToUpload;
    final String audioUrlToUpload;
    final Post post;
}
