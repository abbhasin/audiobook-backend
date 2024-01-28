package com.enigma.audiobook.backend.models.requests;

import com.enigma.audiobook.backend.models.Post;
import lombok.Data;

@Data
public class PostInitRequest {
    Post post;
    UploadInitReq uploadInitReq;
}
