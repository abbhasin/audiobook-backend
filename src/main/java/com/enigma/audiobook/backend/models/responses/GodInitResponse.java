package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.models.God;
import lombok.Data;

@Data
public class GodInitResponse {
    final String imageUrlToUpload;
    final God god;
}
