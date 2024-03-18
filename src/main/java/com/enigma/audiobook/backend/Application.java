package com.enigma.audiobook.backend;

import com.enigma.audiobook.backend.jobs.ContentEncodingScheduler;
import com.enigma.audiobook.backend.jobs.CuratedDarshanHandler;
import com.enigma.audiobook.backend.jobs.CuratedFeedHandler;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Application {
    ContentEncodingScheduler contentEncodingScheduler;
    CuratedDarshanHandler curatedDarshanHandler;
    CuratedFeedHandler curatedFeedHandler;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStarted() {
        contentEncodingScheduler.start();
    }

    // every day at 10pm
    @Scheduled(cron = "0 0 22 * * ?")
    public void runCuratedDarshans() {
        curatedDarshanHandler.run();
    }

    // every day at 10pm
    @Scheduled(cron = "0 0 22 * * ?")
    public void runCuratedFeed() {
        curatedFeedHandler.run();
    }

}
