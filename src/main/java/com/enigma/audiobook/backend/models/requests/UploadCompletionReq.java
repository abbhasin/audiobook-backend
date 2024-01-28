package com.enigma.audiobook.backend.models.requests;

import lombok.Data;

import java.util.List;

@Data
public class UploadCompletionReq {
    List<UploadFileCompletionReq> uploadFileCompletionReqs;
}
