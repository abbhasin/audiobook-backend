package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.Mandir;
import lombok.Data;

@Data
public class MandirInitResponse {
    final String imageUrlToUpload;
    final Mandir mandir;
}
