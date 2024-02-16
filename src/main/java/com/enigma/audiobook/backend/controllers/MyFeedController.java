package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.jobs.CuratedFeedHandler;
import com.enigma.audiobook.backend.models.requests.CuratedFeedRequest;
import com.enigma.audiobook.backend.models.requests.GodFeedRequest;
import com.enigma.audiobook.backend.models.requests.InfluencerFeedRequest;
import com.enigma.audiobook.backend.models.requests.MandirFeedRequest;
import com.enigma.audiobook.backend.models.responses.CuratedFeedResponse;
import com.enigma.audiobook.backend.models.responses.FeedPageResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
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

    @PostMapping("/feed/god/page")
    @ResponseBody
    public FeedPageResponse getFeedOfGod(@RequestBody GodFeedRequest godFeedRequest) {
        log.info("feed of god req:{}", godFeedRequest);
        String lastPostId = null;
        if (godFeedRequest.getCuratedFeedPaginationKey() != null &&
                godFeedRequest.getCuratedFeedPaginationKey().getGodFeedePaginationKey() != null) {
            lastPostId = godFeedRequest.getCuratedFeedPaginationKey().getGodFeedePaginationKey().getLastPostId();
        }
        return oneGodService.getFeedOfGod(
                godFeedRequest.getLimit(), godFeedRequest.getGodId(),
                godFeedRequest.getForUserId(), godFeedRequest.isOnlyProcessed(),
                Optional.ofNullable(lastPostId));
    }

    @PostMapping("/feed/mandir/page")
    @ResponseBody
    public FeedPageResponse getFeedOfMandir(@RequestBody MandirFeedRequest mandirFeedRequest) {
        String lastPostId = null;
        if (mandirFeedRequest.getCuratedFeedPaginationKey() != null &&
                mandirFeedRequest.getCuratedFeedPaginationKey().getMandirFeedPaginationKey() != null) {
            lastPostId = mandirFeedRequest.getCuratedFeedPaginationKey().getMandirFeedPaginationKey().getLastPostId();
        }
        return oneGodService.getFeedOfMandir(
                mandirFeedRequest.getLimit(), mandirFeedRequest.getMandirId(),
                mandirFeedRequest.getForUserId(), mandirFeedRequest.isOnlyProcessed(),
                Optional.ofNullable(lastPostId));
    }

    @PostMapping("/feed/influencer/page")
    @ResponseBody
    public FeedPageResponse getFeedOfInfluencer(@RequestBody InfluencerFeedRequest influencerFeedRequest) {
        String lastPostId = null;
        if (influencerFeedRequest.getCuratedFeedPaginationKey() != null &&
                influencerFeedRequest.getCuratedFeedPaginationKey().getInfluencerFeedPaginationKey() != null) {
            lastPostId = influencerFeedRequest.getCuratedFeedPaginationKey().getInfluencerFeedPaginationKey().getLastPostId();
        }
        return oneGodService.getFeedOfInfluencer(
                influencerFeedRequest.getLimit(), influencerFeedRequest.getInfluencerId(),
                influencerFeedRequest.getForUserId(), influencerFeedRequest.isOnlyProcessed(),
                Optional.ofNullable(lastPostId));
    }
}
