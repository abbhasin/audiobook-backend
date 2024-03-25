package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {

    @Autowired
    OneGodService oneGodService;

    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "Successful";
    }

    @GetMapping("/health/init-collections-indexes")
    @ResponseBody
    public void initCollsAndIndexes(
            @RequestHeader("registration-token") String registrationToken) {
        oneGodService.initCollsAndIndexes(registrationToken);
    }
}
