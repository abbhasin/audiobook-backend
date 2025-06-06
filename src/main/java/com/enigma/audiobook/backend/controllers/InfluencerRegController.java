package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.Influencer;
import com.enigma.audiobook.backend.models.requests.InfluencerContentUploadReq;
import com.enigma.audiobook.backend.models.requests.InfluencerInitRequest;
import com.enigma.audiobook.backend.models.responses.InfluencerCompletionResponse;
import com.enigma.audiobook.backend.models.responses.InfluencerForUser;
import com.enigma.audiobook.backend.models.responses.InfluencerInitResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
public class InfluencerRegController {
    @Autowired
    OneGodService oneGodService;

    @PostMapping("/influencer/initialization")
    @ResponseBody
    public InfluencerInitResponse initInfluencer(
            @RequestHeader("registration-token") String registrationToken,
            @RequestBody InfluencerInitRequest influencerInitRequest) {
        return oneGodService.initInfluencer(influencerInitRequest, registrationToken);
    }

    @PostMapping("/influencers/update-completion")
    @ResponseBody
    public InfluencerCompletionResponse postUploadUpdateInfluencer(
            @RequestHeader("registration-token") String registrationToken,
            @RequestBody InfluencerContentUploadReq influencerContentUploadReq) {
        return oneGodService.postUploadUpdateInfluencer(influencerContentUploadReq, registrationToken);
    }

    @GetMapping("/influencers")
    @ResponseBody
    public List<Influencer> getinfluencers(@RequestParam("limit") int limit) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getInfluencers(limit);
    }

    @GetMapping("/influencers/pagination")
    @ResponseBody
    public List<Influencer> getinfluencersNextPage(@RequestParam("limit") int limit,
                                                   @RequestParam("lastInfluencerId") String lastInfluencerId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getNextPageOfInfluencers(limit, lastInfluencerId);
    }

    @GetMapping("/influencers/users")
    @ResponseBody
    public List<InfluencerForUser> getinfluencersForUser(@RequestParam("limit") int limit,
                                                         @RequestParam("userId") String userId,
                                                         @RequestParam(value = "onlyFollowed",
                                                                 required = false,
                                                                 defaultValue = "false") boolean onlyFollowed) {
        limit = (limit == 0) ? 10 : limit;
        if(onlyFollowed) {
            return oneGodService.getFollowedInfluencersForUser(limit, userId);
        }
        return oneGodService.getInfluencersForUser(limit, userId);
    }

    @GetMapping("/influencers/users/pagination")
    @ResponseBody
    public List<InfluencerForUser> getinfluencersForUserNextPage(@RequestParam("limit") int limit,
                                                                 @RequestParam("lastInfluencerId") String lastInfluencerId,
                                                                 @RequestParam("userId") String userId,
                                                                 @RequestParam(value = "onlyFollowed",
                                                                         required = false,
                                                                         defaultValue = "false") boolean onlyFollowed) {
        limit = (limit == 0) ? 10 : limit;
        if(onlyFollowed) {
            return Collections.emptyList();
        }
        return oneGodService.getNextPageOfInfluencersForUser(limit, lastInfluencerId, userId);
    }

    @GetMapping("/influencers/{userId}")
    @ResponseBody
    public Influencer getinfluencer(@PathVariable("userId") String userId) {
        return oneGodService.getInfluencer(userId);
    }
}
