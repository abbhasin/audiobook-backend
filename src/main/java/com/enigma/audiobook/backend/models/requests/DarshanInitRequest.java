package com.enigma.audiobook.backend.models.requests;

import com.enigma.audiobook.backend.models.Darshan;
import lombok.Data;

@Data
public class DarshanInitRequest {
    Darshan darshan;
    UploadInitReq uploadInitReq;
}
