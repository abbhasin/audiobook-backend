package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.aws.S3UploadHandler;
import com.enigma.audiobook.backend.aws.models.MPURequestStatus;
import com.enigma.audiobook.backend.models.requests.UploadCompletionReq;
import com.enigma.audiobook.backend.models.requests.UploadFileCompletionReq;
import com.enigma.audiobook.backend.models.requests.UploadFileInitReq;
import com.enigma.audiobook.backend.models.requests.UploadInitReq;
import com.enigma.audiobook.backend.models.responses.UploadCompletionRes;
import com.enigma.audiobook.backend.models.responses.UploadFileCompletionRes;
import com.enigma.audiobook.backend.models.responses.UploadFileInitRes;
import com.enigma.audiobook.backend.models.responses.UploadInitRes;
import com.enigma.audiobook.backend.utils.ContentUploadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class UploadController {

    @Autowired
    S3UploadHandler uploadHandler;
    @Autowired
    ContentUploadUtils contentUploadUtils;

    @PostMapping("/uploads/init")
    @ResponseBody
    public UploadInitRes initUpload(@RequestBody UploadInitReq uploadInitReq) {
        log.info("upload init request:" + uploadInitReq);
        UploadInitRes initRes = new UploadInitRes();
        initRes.setRequestStatus(MPURequestStatus.COMPLETED);

        // TODO: add total files size validation and number of files validation
        Map<String, UploadFileInitRes> fileNameToUploadFileResponse = new HashMap<>();

        for (UploadFileInitReq uploadFileInitReq : uploadInitReq.getUploadFileInitReqs()) {
            UploadFileInitRes fileInitRes = contentUploadUtils.initFile(uploadFileInitReq);
            fileNameToUploadFileResponse.put(uploadFileInitReq.getFileName(), fileInitRes);

            if (!fileInitRes.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
                initRes.setRequestStatus(fileInitRes.getRequestStatus());
                initRes.setAbortedReason(fileInitRes.getAbortedReason());
                break;
            }
        }

        initRes.setFileNameToUploadFileResponse(fileNameToUploadFileResponse);

        return initRes;
    }



    @PostMapping("/uploads/completion")
    @ResponseBody
    public UploadCompletionRes completeUpload(@RequestBody UploadCompletionReq uploadCompletionReq) {
        UploadCompletionRes res = new UploadCompletionRes();
        res.setRequestStatus(MPURequestStatus.COMPLETED);

        Map<String, UploadFileCompletionRes> fileNameToUploadFileResponse = new HashMap<>();
        for (UploadFileCompletionReq uploadFileCompletionReq : uploadCompletionReq.getUploadFileCompletionReqs()) {
            UploadFileCompletionRes fileCompletionRes = contentUploadUtils.completeUpload(uploadFileCompletionReq,
                    ContentUploadUtils.ContentTypeByExtension.VIDEO);
            fileNameToUploadFileResponse.put(uploadFileCompletionReq.getFileName(), fileCompletionRes);

            if (!fileCompletionRes.getS3MPUCompleteResponse().getState().equals(MPURequestStatus.COMPLETED)) {
                res.setRequestStatus(fileCompletionRes.getS3MPUCompleteResponse().getState());
                res.setAbortedReason(fileCompletionRes.getS3MPUCompleteResponse().getAbortedReason());

                contentUploadUtils.abortAllCompletionReq(uploadCompletionReq);
                break;
            }
        }

        res.setFileNameToUploadFileResponse(fileNameToUploadFileResponse);

        return res;
    }


}
