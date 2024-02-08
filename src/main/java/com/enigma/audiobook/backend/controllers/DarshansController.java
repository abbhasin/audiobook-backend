package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.jobs.CuratedDarshanHandler;
import com.enigma.audiobook.backend.models.Darshan;
import com.enigma.audiobook.backend.models.requests.DarshanContentUploadReq;
import com.enigma.audiobook.backend.models.requests.DarshanInitRequest;
import com.enigma.audiobook.backend.models.responses.DarshanCompletionResponse;
import com.enigma.audiobook.backend.models.responses.DarshanInitResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DarshansController {
    @Autowired
    OneGodService oneGodService;
    @Autowired
    CuratedDarshanHandler curatedDarshanHandler;

    @PostMapping("/darshans/initialization")
    @ResponseBody
    public DarshanInitResponse initDarshan(@RequestBody DarshanInitRequest darshanInitRequest) {
        return oneGodService.initDarshan(darshanInitRequest);
    }

    @PostMapping("/darshans/update-completion")
    @ResponseBody
    public DarshanCompletionResponse postUploadUpdateDarshan(@RequestBody DarshanContentUploadReq darshanContentUploadReq) {
        return oneGodService.postUploadUpdateDarshan(darshanContentUploadReq);
    }

    @GetMapping("/darshans")
    @ResponseBody
    public List<Darshan> getDarshans() {
        return oneGodService.getCuratedDarshans();
    }

    @PostMapping("/darshans/curation")
    public void invokeCurationOfDarshan() {
        curatedDarshanHandler.run();
    }
}
