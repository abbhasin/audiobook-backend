package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.Darshan;
import com.enigma.audiobook.backend.models.requests.DarshanContentUploadReq;
import com.enigma.audiobook.backend.models.responses.DarshanInitResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DarshansController {
    @Autowired
    OneGodService oneGodService;

    @PostMapping("/darshans/initialization")
    @ResponseBody
    public DarshanInitResponse initDarshan(@RequestBody Darshan darshan) {

        return oneGodService.initDarshan(darshan);
    }

    @PostMapping("/darshans/update-completion")
    @ResponseBody
    public Darshan postUploadUpdateGod(@RequestBody DarshanContentUploadReq darshanContentUploadReq) {
        return oneGodService.postUploadUpdateDarshan(darshanContentUploadReq);
    }

    @GetMapping("/darshans")
    @ResponseBody
    public List<Darshan> getDarshans() {
        return oneGodService.getCuratedDarshans();
    }
}
