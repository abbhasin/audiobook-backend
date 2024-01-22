package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.Following;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FollowingsController {

    @Autowired
    OneGodService oneGodService;

    @PostMapping("/followings/follow")
    public void addFollowing(@RequestBody Following following) {
        oneGodService.addFollowing(following);
    }

    @PostMapping("/followings/unfollow")
    public void removeFollowing(@RequestBody Following following) {
        oneGodService.removeFollowing(following);
    }

    @PostMapping("/followings/{userId}")
    public List<Following> removeFollowing(@PathVariable("userId") String userId) {
        return oneGodService.getFollowingsForUser(userId);
    }


>>>>>>> 73953ad (z)
}
