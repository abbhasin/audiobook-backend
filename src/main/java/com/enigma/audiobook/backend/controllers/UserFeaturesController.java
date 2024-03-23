package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.User;
import com.enigma.audiobook.backend.service.OneGodService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class UserFeaturesController {

    @Autowired
    OneGodService oneGodService;

    @PostMapping("/user-features/swipe-of-darshans")
    public void swipedDarshanAtLeastOnce(@RequestBody User user) {
        oneGodService.swipedDarshansAtLeastOnce(user.getUserId());
    }

    @GetMapping("/user-features/swipe-of-darshans")
    @ResponseBody
    public boolean isSwipeDarshanPugAnimationEnabled(@RequestParam("userId") String userId) {
        return oneGodService.isSwipedDarshansPugAnimationEnabled(userId);
    }
}
