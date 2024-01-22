package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FollowingsController {

    @Autowired
    OneGodService oneGodService;
}
