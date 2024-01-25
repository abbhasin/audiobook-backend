package com.enigma.audiobook.backend.aws;

import com.enigma.audiobook.backend.aws.models.MPURequestStatus;
import com.enigma.audiobook.backend.aws.models.S3MPUPreSignedUrlsResponse;
import com.enigma.audiobook.backend.controllers.UploadController;
import com.enigma.audiobook.backend.proxies.RestClient;
import com.enigma.audiobook.backend.utils.SerDe;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class S3MPUClientMain {
    RestClient restClient = new RestClient();
    SerDe serDe = new SerDe();

    private void uploadFile(String filePath) {


        File file = new File(filePath);
        UploadController.UploadInitRes response = initUpload(file);

        if (!response.getRequestStatus().equals(MPURequestStatus.COMPLETED)) {
            log.error("upload init request failed:" + response);
            return;
        }

        log.info("upload init response:" + response);
        Map<String, UploadController.UploadFileInitRes> fileNameToUploadFileResponse =
                response.getFileNameToUploadFileResponse();
        UploadController.UploadFileInitRes fileInitRes = fileNameToUploadFileResponse.get(file.getName());
        S3MPUPreSignedUrlsResponse s3MPUPreSignedUrlsResponse = fileInitRes.getS3MPUPreSignedUrlsResponse();
        long chunkSize = s3MPUPreSignedUrlsResponse.getChunkSize();
        long totalNumOfParts = s3MPUPreSignedUrlsResponse.getTotalNumOfParts();
        Map<Integer, String> partNumToUrl = s3MPUPreSignedUrlsResponse.getPartNumToUrl();
        


    }

    private UploadController.UploadInitRes initUpload(File file) {
        String URL_PREFIX = "localhost:8080";
        String URL_PATH_UPLOAD_INIT = "uploads/init";

        UploadController.UploadInitReq uploadInitReq = new UploadController.UploadInitReq();
        List<UploadController.UploadFileInitReq> uploadFileInitReqs = new ArrayList<>();

        UploadController.UploadFileInitReq uploadFileInitReq = new UploadController.UploadFileInitReq();
        uploadFileInitReq.setFileName(file.getName());
        uploadFileInitReq.setTotalSize(file.getTotalSpace());

        uploadFileInitReqs.add(uploadFileInitReq);
        uploadInitReq.setUploadFileInitReqs(uploadFileInitReqs);

        UploadController.UploadInitRes response = restClient.doPost(URL_PREFIX, URL_PATH_UPLOAD_INIT, serDe.toJson(uploadInitReq),
                UploadController.UploadInitRes.class);
        return response;
    }
}
