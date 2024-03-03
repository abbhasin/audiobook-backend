package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.responses.MorePages;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MorePagesController {

    @Autowired
    OneGodService oneGodService;

    @GetMapping("/more-pages")
    @ResponseBody
    public MorePages getMorePages(@RequestParam("userId") String userId) {
        MorePages morePages = new MorePages();
        morePages.setPage(oneGodService.getAuthorizedPagesForUser(userId));
        return morePages;
    }

}
