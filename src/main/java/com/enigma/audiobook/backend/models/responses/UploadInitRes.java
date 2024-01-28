package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.aws.models.MPUAbortedReason;
import com.enigma.audiobook.backend.aws.models.MPURequestStatus;
import lombok.Data;

import java.util.Map;

@Data
public class UploadInitRes {
    Map<String, UploadFileInitRes> fileNameToUploadFileResponse;
    MPURequestStatus requestStatus;
    MPUAbortedReason abortedReason;
}
