package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    @Autowired
    OneGodService oneGodService;

    @PostMapping("/authorization/mandirs")
    @ResponseBody
    public void addMandirAuth(
            @RequestHeader("registration-token") String registrationToken,
            @RequestParam("mandirId") String mandirId, @RequestParam("userId") String userId) {
        oneGodService.addMandirAuth(mandirId, userId, registrationToken);
    }

    @PostMapping("/authorization/gods")
    @ResponseBody
    public void addGodAuth(
            @RequestHeader("registration-token") String registrationToken,
            @RequestParam("godId") String godId, @RequestParam("userId") String userId) {
        oneGodService.addGodAuth(godId, userId, registrationToken);
    }

    @PostMapping("/authorization/influencers")
    @ResponseBody
    public void addInfluencerAuth(
            @RequestHeader("registration-token") String registrationToken,
            @RequestParam("influencerId") String influencerId, @RequestParam("userId") String userId) {
        oneGodService.addInfluencerAuth(influencerId, userId, registrationToken);
    }
}
