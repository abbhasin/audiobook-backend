package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.models.PostAssociationType;
import com.enigma.audiobook.backend.models.requests.PostContentUploadReq;
import com.enigma.audiobook.backend.models.responses.PostInitResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PostsController {
    @Autowired
    OneGodService oneGodService;

    @PostMapping("/posts/initialization")
    @ResponseBody
    public PostInitResponse initPost(@RequestBody Post posts) {
        return oneGodService.initPosts(posts);
    }

    @PostMapping("/posts/update-completion")
    @ResponseBody
    public Post postUploadUpdatePost(@RequestBody PostContentUploadReq postContentUploadReq) {
        return oneGodService.postUploadUpdatePost(postContentUploadReq);
    }

    @GetMapping("/posts/mandir/{mandirId}")
    @ResponseBody
    public List<Post> getPostsByMandir(@RequestParam("limit") int limit,
                                       @PathVariable("mandirId") String mandirId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getPosts(limit, mandirId, PostAssociationType.MANDIR);
    }

    @GetMapping("/posts/infleuncer/{influencerId}")
    @ResponseBody
    public List<Post> getPostsByInfluencer(@RequestParam("limit") int limit,
                                           @PathVariable("influencerId") String influencerId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getPosts(limit, influencerId, PostAssociationType.INFLUENCER);
    }

    @GetMapping("/posts/god/{godId}")
    @ResponseBody
    public List<Post> getPostsByGod(@RequestParam("limit") int limit,
                                    @PathVariable("godId") String godId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getPosts(limit, godId, PostAssociationType.GOD);
    }

    @GetMapping("/posts/mandir/pagination/{mandirId}")
    @ResponseBody
    public List<Post> getPostsByMandirNext(@RequestParam("limit") int limit,
                                           @RequestParam("lastPostId") String lastPostId,
                                           @PathVariable("mandirId") String mandirId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getPostsPaginated(limit, mandirId,
                PostAssociationType.MANDIR, lastPostId);
    }

    @GetMapping("/posts/infleuncer/pagination/{influencerId}")
    @ResponseBody
    public List<Post> getPostsByInfluencerNext(@RequestParam("limit") int limit,
                                               @RequestParam("lastPostId") String lastPostId,
                                               @PathVariable("influencerId") String influencerId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getPostsPaginated(limit, influencerId,
                PostAssociationType.INFLUENCER, lastPostId);
    }

    @GetMapping("/posts/god/pagination/{godId}")
    @ResponseBody
    public List<Post> getPostsByGodNext(@RequestParam("limit") int limit,
                                        @RequestParam("lastPostId") String lastPostId,
                                        @PathVariable("godId") String godId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getPostsPaginated(limit, godId,
                PostAssociationType.GOD, lastPostId);
    }

    @GetMapping("/posts/{postId}")
    @ResponseBody
    public Post getPost(@PathVariable("postId") String postId) {
        return oneGodService.getPost(postId);
    }
}
