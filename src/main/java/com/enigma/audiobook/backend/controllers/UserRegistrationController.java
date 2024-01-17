package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.User;
import com.enigma.audiobook.backend.models.requests.UserRegistrationInfo;
import com.enigma.audiobook.backend.models.responses.UserAssociationResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserRegistrationController {
    @Autowired
    OneGodService oneGodService;

    @PostMapping("/users")
    @ResponseBody
    public User createUser() {
        return oneGodService.createUser();
    }

    @PostMapping("/users/associations")
    @ResponseBody
    public UserAssociationResponse associateAuthenticatedUser(@RequestBody UserRegistrationInfo userRegistrationInfo) {
        return oneGodService.associateAuthenticatedUser(userRegistrationInfo);
    }

    @GetMapping("/users/{userId}")
    @ResponseBody
    public User getUser(@PathVariable("userId") String userId) {
        return oneGodService.getUser(userId);
    }

}
