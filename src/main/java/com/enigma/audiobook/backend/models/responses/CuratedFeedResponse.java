package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.Post;
import lombok.Data;

import java.util.List;

@Data
public class CuratedFeedResponse {
    List<Post> posts;
    CuratedFeedPaginationKey curatedFeedPaginationKey;
}
