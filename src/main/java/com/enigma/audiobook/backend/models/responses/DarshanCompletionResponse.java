package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.Darshan;
import lombok.Data;

@Data
public class DarshanCompletionResponse {
    final Darshan darshan;
    final UploadCompletionRes uploadCompletionRes;
}
