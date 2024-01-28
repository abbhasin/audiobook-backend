package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.Post;
import lombok.Data;

@Data
public class PostCompletionResponse {
    final Post post;
    final UploadCompletionRes uploadCompletionRes;
}
