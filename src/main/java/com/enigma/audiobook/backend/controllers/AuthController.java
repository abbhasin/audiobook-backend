package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @Autowired
    OneGodService oneGodService;

    @PostMapping("/authorization/mandirs")
    @ResponseBody
    public void addMandirAuth(@RequestParam("mandirId") String mandirId, @RequestParam("userId") String userId) {
        oneGodService.addMandirAuth(mandirId, userId);
    }

    @PostMapping("/authorization/gods")
    @ResponseBody
    public void addGodAuth(@RequestParam("godId") String godId, @RequestParam("userId") String userId) {
        oneGodService.addGodAuth(godId, userId);
    }

    @PostMapping("/authorization/influencers")
    @ResponseBody
    public void addInfluencerAuth(@RequestParam("influencerId") String influencerId, @RequestParam("userId") String userId) {
        oneGodService.addInfluencerAuth(influencerId, userId);
    }
}
