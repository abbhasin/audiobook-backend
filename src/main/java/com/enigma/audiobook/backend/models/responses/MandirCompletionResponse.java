package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.Mandir;
import lombok.Data;

@Data
public class MandirCompletionResponse {
    final Mandir mandir;
    final UploadCompletionRes uploadCompletionRes;
}
