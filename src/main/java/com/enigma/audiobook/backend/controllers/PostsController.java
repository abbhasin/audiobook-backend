package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.Post;
import com.enigma.audiobook.backend.models.PostAssociationType;
import com.enigma.audiobook.backend.models.requests.PostContentUploadReq;
import com.enigma.audiobook.backend.models.requests.PostInitRequest;
import com.enigma.audiobook.backend.models.responses.PostCompletionResponse;
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
    public PostInitResponse initPost(@RequestHeader("user-auth-token") String userAuthToken,
                                     @RequestBody PostInitRequest postInitReq) {
        return oneGodService.initPosts(userAuthToken, postInitReq);
    }

    @PostMapping("/posts/update-completion")
    @ResponseBody
    public PostCompletionResponse postUploadUpdatePost(@RequestHeader("user-auth-token") String userAuthToken,
                                                       @RequestBody PostContentUploadReq postContentUploadReq) {
        return oneGodService.postUploadUpdatePost(userAuthToken, postContentUploadReq);
    }

    @GetMapping("/posts/mandir/{mandirId}")
    @ResponseBody
    public List<Post> getPostsByMandir(@RequestParam("limit") int limit,
                                       @PathVariable("mandirId") String mandirId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getPosts(limit, mandirId, PostAssociationType.MANDIR);
    }

    @GetMapping("/posts/influencer/{influencerId}")
    @ResponseBody
    public List<Post> getPostsByInfluencer(@RequestParam("limit") int limit,
                                           @RequestParam("onlyProcessed") boolean onlyProcessed,
                                           @PathVariable("influencerId") String influencerId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getPostsOfInfluencer(limit, influencerId, onlyProcessed);
    }

    @GetMapping("/posts/god/{godId}")
    @ResponseBody
    public List<Post> getPostsByGod(@RequestParam("limit") int limit,
                                    @PathVariable("godId") String godId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getPostsOfGod(limit, godId);
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
                                               @RequestParam("onlyProcessed") boolean onlyProcessed,
                                               @PathVariable("influencerId") String influencerId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getPostsByInfluencerPaginated(limit, influencerId, lastPostId, onlyProcessed);
    }

    @GetMapping("/posts/god/pagination/{godId}")
    @ResponseBody
    public List<Post> getPostsByGodNext(@RequestParam("limit") int limit,
                                        @RequestParam("lastPostId") String lastPostId,
                                        @PathVariable("godId") String godId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getPostsOfGodPaginated(limit, godId, lastPostId);
    }

    @GetMapping("/posts/{postId}")
    @ResponseBody
    public Post getPost(@PathVariable("postId") String postId) {
        return oneGodService.getPost(postId);
    }
}
