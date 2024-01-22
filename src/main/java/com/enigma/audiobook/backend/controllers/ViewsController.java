package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.View;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ViewsController {

    @Autowired
    OneGodService oneGodService;

    @PostMapping("/views")
    public void addViewForUser(@RequestBody View view) {
        oneGodService.addViewForUser(view);
    }
}
