package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.Influencer;
import com.enigma.audiobook.backend.models.requests.InfluencerImageUploadReq;
import com.enigma.audiobook.backend.models.responses.InfluencerInitResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InfluencerRegController {
    @Autowired
    OneGodService oneGodService;

    @PostMapping("/influencer/initialization")
    @ResponseBody
    public InfluencerInitResponse initInfluencer(@RequestBody Influencer influencer) {
        return oneGodService.initInfluencer(influencer);
    }

    @PostMapping("/influencers/update-completion")
    @ResponseBody
    public Influencer postUploadUpdateInfluencer(@RequestBody InfluencerImageUploadReq influencerImageUploadReq) {
        return oneGodService.postUploadUpdateInfluencer(influencerImageUploadReq);
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

    @GetMapping("/influencers/{userId}")
    @ResponseBody
    public Influencer getinfluencer(@PathVariable("userId") String userId) {
        return oneGodService.getInfluencer(userId);
    }
}
