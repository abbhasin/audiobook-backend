package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.dao.DarshanDao;
import com.enigma.audiobook.backend.dao.PostsDao;
import com.enigma.audiobook.backend.jobs.ContentEncodingHandler;
import com.enigma.audiobook.backend.models.Darshan;
import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.utils.SerDe;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContentTransformerController {
    @Autowired
    ContentEncodingHandler contentEncodingHandler;
    private static final SerDe serDe = new SerDe();

    @PostMapping("/content-transformation/posts")
    @ResponseBody
    public void transformContentPost(@RequestBody ContentTransformationPostRequest contentTransformationPostRequest) {
        contentEncodingHandler.encodeContentForCollectionEntry("dev", PostsDao.POSTS_COLLECTION,
                serDe.toJson(contentTransformationPostRequest.getPost()));
    }

    @PostMapping("/content-transformation/darshan")
    @ResponseBody
    public void transformContentDarshan(@RequestBody ContentTransformationDarshanRequest contentTransformationDarshanRequest) {
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
