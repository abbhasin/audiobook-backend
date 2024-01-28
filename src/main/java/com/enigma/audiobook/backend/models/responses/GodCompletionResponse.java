package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.God;
import lombok.Data;

@Data
public class GodCompletionResponse {
    final God god;
    final UploadCompletionRes uploadCompletionRes;
}
