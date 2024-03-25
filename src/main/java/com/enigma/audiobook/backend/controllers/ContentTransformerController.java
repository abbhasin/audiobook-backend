package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.dao.DarshanDao;
import com.enigma.audiobook.backend.dao.PostsDao;
import com.enigma.audiobook.backend.jobs.ContentEncodingHandler;
import com.enigma.audiobook.backend.models.Darshan;
import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.service.OneGodService;
import com.enigma.audiobook.backend.utils.SerDe;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ContentTransformerController {
    @Autowired
    ContentEncodingHandler contentEncodingHandler;
    @Autowired
    OneGodService oneGodService;
    private static final SerDe serDe = new SerDe();

    @PostMapping("/content-transformation/posts")
    @ResponseBody
    public void transformContentPost(
            @RequestHeader("registration-token") String registrationToken,
            @RequestBody ContentTransformationPostRequest contentTransformationPostRequest) {
        oneGodService.checkValidRegistrationToken(registrationToken);
        contentEncodingHandler.encodeContentForCollectionEntry("dev", PostsDao.POSTS_COLLECTION,
                serDe.toJson(contentTransformationPostRequest.getPost()));
    }

    @PostMapping("/content-transformation/darshan")
    @ResponseBody
    public void transformContentDarshan(
            @RequestHeader("registration-token") String registrationToken,
            @RequestBody ContentTransformationDarshanRequest contentTransformationDarshanRequest) {
        oneGodService.checkValidRegistrationToken(registrationToken);
        contentEncodingHandler.encodeContentForCollectionEntry("dev", DarshanDao.DARSHAN_REG_COLLECTION,
                serDe.toJson(contentTransformationDarshanRequest.getDarshan()));
    }

    @Data
    public static class ContentTransformationPostRequest {
        Post post;
    }

    @Data
    public static class ContentTransformationDarshanRequest {
        Darshan darshan;
    }
}
