package com.enigma.audiobook.backend.models.responses;

import com.enigma.audiobook.backend.aws.models.MPUAbortedReason;
import com.enigma.audiobook.backend.aws.models.MPURequestStatus;
import lombok.Data;

import java.util.Map;

@Data
public class UploadCompletionRes {
    Map<String, UploadFileCompletionRes> fileNameToUploadFileResponse;
    MPURequestStatus requestStatus;
    MPUAbortedReason abortedReason;
}
