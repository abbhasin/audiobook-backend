package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.User;
import com.enigma.audiobook.backend.models.requests.UserRegistrationInfo;
import com.enigma.audiobook.backend.models.responses.UserAssociationResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UserRegistrationController {
    @Autowired
    OneGodService oneGodService;

    @PostMapping("/users")
    @ResponseBody
    public User createUser(@RequestHeader Map<String, String> headers,
                           HttpServletRequest servletRequest) {
        return oneGodService.createUser(headers, servletRequest.getRemoteAddr());
    }

    @PostMapping("/users/associations")
    @ResponseBody
    public UserAssociationResponse associateAuthenticatedUser(@RequestBody UserRegistrationInfo userRegistrationInfo,
                                                              @RequestHeader Map<String, String> headers,
                                                              HttpServletRequest servletRequest) {
        return oneGodService.associateAuthenticatedUser(userRegistrationInfo, headers, servletRequest.getRemoteAddr());
    }

    @GetMapping("/users/{userId}")
    @ResponseBody
    public User getUser(@PathVariable("userId") String userId) {
        return oneGodService.getUser(userId);
    }

}
