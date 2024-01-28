package com.enigma.audiobook.backend.controllers;

import com.enigma.audiobook.backend.models.Mandir;
import com.enigma.audiobook.backend.models.requests.MandirContentUploadReq;
import com.enigma.audiobook.backend.models.requests.MandirInitRequest;
import com.enigma.audiobook.backend.models.responses.MandirCompletionResponse;
import com.enigma.audiobook.backend.models.responses.MandirInitResponse;
import com.enigma.audiobook.backend.service.OneGodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MandirController {
    @Autowired
    OneGodService oneGodService;

    @PostMapping("/mandirs/initialization")
    @ResponseBody
    public MandirInitResponse initMandir(@RequestBody MandirInitRequest mandirInitRequest) {
        return oneGodService.initMandir(mandirInitRequest);
    }

    @PostMapping("/mandirs/update-completion")
    @ResponseBody
    public MandirCompletionResponse postUploadUpdateMandir(@RequestBody MandirContentUploadReq mandirContentUploadReq) {
        return oneGodService.postUploadUpdateMandir(mandirContentUploadReq);
    }

    @GetMapping("/mandirs")
    @ResponseBody
    public List<Mandir> getMandirs(@RequestParam("limit") int limit) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getMandirs(limit);
    }

    @GetMapping("/mandirs/pagination")
    @ResponseBody
    public List<Mandir> getMandirsNextPage(@RequestParam("limit") int limit, @RequestParam("lastMandirId") String lastMandirId) {
        limit = (limit == 0) ? 10 : limit;
        return oneGodService.getMandirsPaginated(limit, lastMandirId);
    }

    @GetMapping("/mandirs/{godId}")
    @ResponseBody
    public Mandir getMandir(@PathVariable("mandirId") String mandirId) {
        return oneGodService.getMandir(mandirId);
    }

    @PostMapping("/mandirs/authorization")
    @ResponseBody
    public void addMandirAuth(@RequestParam("mandirId") String mandirId, @RequestParam("userId") String userId) {
        oneGodService.addMandirAuth(mandirId, userId);
    }

    @GetMapping("/mandirs/{userId}")
    @ResponseBody
    public List<Mandir> addMandirAuth(@PathVariable("userId") String userId) {
        return oneGodService.getAuthorizedMandirForUser(userId);
    }
}
