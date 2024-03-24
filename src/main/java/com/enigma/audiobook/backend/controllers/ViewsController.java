package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.DarshanView;
import com.enigma.audiobook.backend.models.View;
import com.enigma.audiobook.backend.service.OneGodService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ViewsController {

    @Autowired
    OneGodService oneGodService;

    @PostMapping("/views/posts")
    public void addViewForUser(@RequestBody View view) {
        log.info("views request:" + view);
        oneGodService.addViewForUser(view);
    }


    @PostMapping("/views/darshans")
    public void addDarshanViewForUser(@RequestBody DarshanView view) {
        log.info("darshan views request:" + view);
        oneGodService.addDarshanViewForUser(view);
    }
}
