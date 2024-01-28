package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.God;
import com.enigma.audiobook.backend.models.requests.GodContentUploadReq;
import com.enigma.audiobook.backend.models.requests.GodInitRequest;
import com.enigma.audiobook.backend.models.responses.GodCompletionResponse;
import com.enigma.audiobook.backend.models.responses.GodInitResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/gods/{godId}")
    @ResponseBody
    public God getGod(@PathVariable("godId") String godId) {
        return oneGodService.getGod(godId);
    }
}
