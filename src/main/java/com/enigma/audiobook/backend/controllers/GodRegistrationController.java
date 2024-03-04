package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.God;
import com.enigma.audiobook.backend.models.requests.GodContentUploadReq;
import com.enigma.audiobook.backend.models.requests.GodInitRequest;
import com.enigma.audiobook.backend.models.responses.GodCompletionResponse;
import com.enigma.audiobook.backend.models.responses.GodForUser;
import com.enigma.audiobook.backend.models.responses.GodInitResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
public class GodRegistrationController {
    @Autowired
    OneGodService oneGodService;

    @PostMapping("/gods/initialization")
    @ResponseBody
    public GodInitResponse initGod(@RequestBody GodInitRequest godInitRequest) {
        return oneGodService.initGod(godInitRequest);
    }

    @PostMapping("/gods/update-completion")
    @ResponseBody
    public GodCompletionResponse postUploadUpdateGod(@RequestBody GodContentUploadReq godContentUploadReq) {
        return oneGodService.postUploadUpdateGod(godContentUploadReq);
    }

    @GetMapping("/gods")
    @ResponseBody
    public List<God> getGods(@RequestParam("limit") int limit) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getGods(limit);
    }

    @GetMapping("/gods/pagination")
    @ResponseBody
    public List<God> getGodsNextPage(@RequestParam("limit") int limit, @RequestParam("lastGodId") String lastGodId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getNextPageOfGods(limit, lastGodId);
    }

    @GetMapping("/gods/users")
    @ResponseBody
    public List<GodForUser> getGodsForUser(@RequestParam("limit") int limit,
                                           @RequestParam("userId") String userId,
                                           @RequestParam(value = "onlyFollowed",
                                                   required = false,
                                                   defaultValue = "false") boolean onlyFollowed) {
        limit = (limit == 0) ? 10 : limit;
        if (onlyFollowed) {
            return oneGodService.getFollowedGodsForUser(limit, userId);
        }
        return oneGodService.getGodsForUser(limit, userId);
    }

    @GetMapping("/gods/users/pagination")
    @ResponseBody
    public List<GodForUser> getGodsForUserNextPage(@RequestParam("limit") int limit,
                                                   @RequestParam("lastGodId") String lastGodId,
                                                   @RequestParam("userId") String userId,
                                                   @RequestParam(value = "onlyFollowed",
                                                           required = false,
                                                           defaultValue = "false") boolean onlyFollowed) {
        limit = (limit == 0) ? 10 : limit;
        if (onlyFollowed) {
            return Collections.emptyList();
            //oneGodService.getFollowedGodsForUser(limit, userId);
        }
        return oneGodService.getNextPageOfGodsForUser(limit, lastGodId, userId);
    }

    @GetMapping("/gods/{godId}")
    @ResponseBody
    public God getGod(@PathVariable("godId") String godId) {
        return oneGodService.getGod(godId);
    }
}
