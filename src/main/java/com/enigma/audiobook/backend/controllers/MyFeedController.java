package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.jobs.CuratedFeedHandler;
import com.enigma.audiobook.backend.models.requests.CuratedFeedRequest;
import com.enigma.audiobook.backend.models.responses.CuratedFeedResponse;
import com.enigma.audiobook.backend.models.responses.FeedPageResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class MyFeedController {
    @Autowired
    OneGodService oneGodService;
    @Autowired
    CuratedFeedHandler curatedFeedHandler;

    @GetMapping("/feed")
    @ResponseBody
    public CuratedFeedResponse getCuratedFeed(@RequestBody CuratedFeedRequest curatedFeedRequest) {
        return oneGodService.getCuratedFeed(curatedFeedRequest);
    }

    @PostMapping("/feed/page")
    @ResponseBody
    public FeedPageResponse getCuratedFeedPage(@RequestBody CuratedFeedRequest curatedFeedRequest) {
        return oneGodService.getCuratedFeedPage(curatedFeedRequest);
    }

    @PostMapping("/feed/curation")
    public void curateFeed() {
        curatedFeedHandler.run();
    }
}
