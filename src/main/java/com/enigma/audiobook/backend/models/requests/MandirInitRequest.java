package com.enigma.audiobook.backend.models.requests;

import com.enigma.audiobook.backend.models.Mandir;
import lombok.Data;

@Data
public class MandirInitRequest {
    Mandir mandir;
    UploadInitReq uploadInitReq;
}
