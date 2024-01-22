package com.enigma.audiobook.backend.models.requests;

import com.enigma.audiobook.backend.models.responses.CuratedFeedPaginationKey;
import lombok.Data;

@Data
public class CuratedFeedRequest {
    String userId;
    int limit;
    CuratedFeedPaginationKey curatedFeedPaginationKey;
}
