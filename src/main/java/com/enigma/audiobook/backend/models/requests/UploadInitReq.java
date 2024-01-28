package com.enigma.audiobook.backend.models.requests;

import lombok.Data;

import java.util.List;

@Data
public class UploadInitReq {
    List<UploadFileInitReq> uploadFileInitReqs;
}
