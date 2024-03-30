package com.enigma.audiobook.backend;

import com.enigma.audiobook.backend.jobs.ContentEncodingScheduler;
import com.enigma.audiobook.backend.jobs.CuratedDarshanHandler;
import com.enigma.audiobook.backend.jobs.CuratedFeedCleanup;
import com.enigma.audiobook.backend.jobs.CuratedFeedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Application {
    @Autowired
    ContentEncodingScheduler contentEncodingScheduler;
    @Autowired
    CuratedDarshanHandler curatedDarshanHandler;
    @Autowired
    CuratedFeedHandler curatedFeedHandler;
    @Autowired
    CuratedFeedCleanup curatedFeedCleanup;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStarted() {
        contentEncodingScheduler.start();
    }

    // every day at 10pm
    @Scheduled(cron = "0 0 22 * * ?", scheduler = "appJobsScheduler")
    public void runCuratedDarshans() {
        curatedDarshanHandler.run();
    }

    // every day at 10pm
    @Scheduled(cron = "0 0 22 * * ?", scheduler = "appJobsScheduler")
    public void runCuratedFeed() {
        curatedFeedHandler.run();
    }

    @Scheduled(cron = "0 0 22 * * ?", scheduler = "appJobsScheduler")
    public void runCuratedFeedCleanup() {
        curatedFeedCleanup.run();
    }

}
