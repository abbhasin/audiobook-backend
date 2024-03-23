package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.aws.S3MPUClientMain;
import com.enigma.audiobook.backend.aws.S3Proxy;
import com.enigma.audiobook.backend.aws.S3UploadHandler;
import com.enigma.audiobook.backend.aws.models.MPURequestStatus;
import com.enigma.audiobook.backend.aws.models.S3MPUCompletedPart;
import com.enigma.audiobook.backend.models.requests.*;
import com.enigma.audiobook.backend.models.responses.*;
import com.enigma.audiobook.backend.service.OneGodService;
import com.enigma.audiobook.backend.utils.ContentUploadUtils;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class UploadController {

    @Autowired
    S3UploadHandler uploadHandler;
    @Autowired
    ContentUploadUtils contentUploadUtils;

    @Autowired
    OneGodService oneGodService;

    @Autowired
    S3Proxy s3Proxy;

    @PostMapping("/uploads/init")
    @ResponseBody
    public UploadInitRes initUpload(
            @RequestHeader("registration-token") String registrationToken,
            @RequestBody UploadInitReq uploadInitReq) {
        log.info("upload init request:" + uploadInitReq);
        oneGodService.checkValidRegistrationToken(registrationToken);
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
    public UploadCompletionRes completeUpload(
            @RequestHeader("registration-token") String registrationToken,
            @RequestBody UploadCompletionReq uploadCompletionReq) {
        oneGodService.checkValidRegistrationToken(registrationToken);
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

    @PostMapping("/uploads/parts")
    @ResponseBody
    public UploadPartsResponse uploadParts(
            @RequestHeader("registration-token") String registrationToken,
            @RequestBody UploadPartsRequest request) {
        List<S3MPUCompletedPart> completedParts =
                S3MPUClientMain.uploadParts(request.getS3MPUPreSignedUrlsResponse(), new File(request.getFilePath()));
        UploadPartsResponse response = new UploadPartsResponse();
        response.setS3MPUCompletedParts(completedParts);
        return response;
    }

    @GetMapping("/uploads/s3-download")
    public void tryS3Download() {
        String fileStr = "/Users/akhil/Downloads/tmp/one-god-local/input/posts/videos/user/65a7936792bb9e2f44a1ea47/video/65c25f30b0ba6251a747e6ee/raw/c2hpdmFfcG9vamFfdmlkZW8=.mp4";

        String inputContentLocalFilePathDir = fileStr.substring(0, fileStr.lastIndexOf("/"));
        log.info("inputContentLocalFilePathDir:{}", inputContentLocalFilePathDir);
        File dir = new File(inputContentLocalFilePathDir);
        log.info("dir exists:{}, can write:{}", dir.exists(), dir.canWrite());
        if (!dir.exists()) {
            boolean dirCreated = dir.mkdirs();
            log.info("created dir:{}", dirCreated);
        }

//        String dirStr2 = "/Users/akhil/Downloads/tmp/one-god-local/input/posts/videos/user/65a7936792bb9e2f44a1ea47/video/65c25f30b0ba6251a747e6ee/raw";
//        File dir2 = new File(dirStr2);
//        log.info("dir2 exists:{}", dir2.exists());
//        if (!dir2.exists()) {
//            boolean created = dir2.mkdirs();
//            log.info("created dirs2:{}", created);
//        }

        s3Proxy.getObject("https://one-god-dev.s3.ap-south-1.amazonaws.com/posts/videos/user/65a7936792bb9e2f44a1ea47/video/65c25f30b0ba6251a747e6ee/raw/c2hpdmFfcG9vamFfdmlkZW8=.mp4",
                new File(fileStr).toURI());

    }


}
