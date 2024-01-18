package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.Darshan;
import lombok.Data;

@Data
public class DarshanInitResponse {
    final String videoUrlToUpload;
    final Darshan darshan;
}
